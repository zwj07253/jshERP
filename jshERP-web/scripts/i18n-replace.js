/**
 * i18n 批量替换脚本
 *
 * 用法: node scripts/i18n-replace.js
 *
 * 功能:
 * 1. 读取映射表 (i18n-map.js)
 * 2. 扫描 src/ 下所有 .vue 和 .js 文件
 * 3. 根据上下文自动替换中文为 $t() 调用
 * 4. 跳过注释、语言包文件
 */

const fs = require('fs')
const path = require('path')

const mapping = require('./i18n-map')

// 按中文长度降序排序，避免短串先匹配导致长串失效
const sortedEntries = Object.entries(mapping).sort((a, b) => b[0].length - a[0].length)

const SRC_DIR = path.resolve(__dirname, '../src')

// 跳过的文件
const SKIP_FILES = ['zh-CN.js', 'en.js', 'i18n-map.js', 'i18n-replace.js']

let totalReplacements = 0
let modifiedFiles = []

/**
 * 判断行是否是注释
 */
function isCommentLine(line, inBlockComment) {
  const trimmed = line.trim()
  if (inBlockComment) return true
  // 单行注释 (支持前面有空格)
  if (trimmed.startsWith('//')) return true
  // 块注释
  if (trimmed.startsWith('/*')) return true
  if (trimmed.startsWith('*')) return true
  // HTML 注释 (整行都是注释)
  if (trimmed.startsWith('<!--') && trimmed.includes('-->')) return true
  // 纯注释行 (包含中文但只有注释内容)
  if (/^\s*\/\//.test(line)) return true
  if (/^\s*\/\*/.test(line)) return true
  if (/^\s*\*/.test(line)) return true
  // 行内注释 (代码后面的 // 注释) - 不跳过整行，但注释部分会被保留
  return false
}

/**
 * 在模板上下文中替换属性
 * label="中文" → :label="$t('key')"
 * placeholder="中文" → :placeholder="$t('key')"
 */
function replaceTemplateAttrs(line) {
  const attrs = ['label', 'placeholder', 'cancelText', 'okText', 'title', 'data-title', 'data-intro', 'content', 'description']
  for (const attr of attrs) {
    // 匹配 attr="含中文的值" 但排除 :attr="..." 已绑定的
    const regex = new RegExp(`(?<!:)${attr}="([^"]*[一-鿿][^"]*)"`, 'g')
    line = line.replace(regex, (match, value) => {
      const trimmed = value.trim()
      const key = mapping[trimmed]
      if (key) {
        return `:${attr}="$t('${key}')"`
      }
      return match
    })
  }
  return line
}

/**
 * 替换 <tag>中文</tag> 模式
 */
function replaceTagContent(line) {
  // <a-button>中文</a-button>, <span>中文</span> 等
  const tagPattern = /(<(?:a-button|a-select-option|a-radio|span|div|h[1-6]|p|label|td|th|a-menu-item)[^>]*>)\s*([一-鿿][^<{}]*?)\s*(<\/)/g
  line = line.replace(tagPattern, (match, openTag, text, closeTag) => {
    const trimmed = text.trim()
    const key = mapping[trimmed]
    if (key) {
      return `${openTag}{{ $t('${key}') }}${closeTag}`
    }
    return match
  })
  return line
}

/**
 * 替换标签间独立中文文本
 * >中文< → >{{ $t('key') }}<
 */
function replaceInlineText(line) {
  // 匹配 >纯中文< 的模式（不含 {{ }}）
  const regex = />([一-鿿][^<{]*?)</g
  line = line.replace(regex, (match, text) => {
    const trimmed = text.trim()
    const key = mapping[trimmed]
    if (key) {
      return `>{{ $t('${key}') }}<`
    }
    return match
  })
  return line
}

/**
 * 替换 {{ }} 内的中文（三元表达式等）
 * {{ condition ? '中文1' : '中文2' }} → {{ condition ? $t('key1') : $t('key2') }}
 */
function replaceMustacheChinese(line) {
  // 匹配 {{ ... '中文' ... }} 模式
  const mustacheRegex = /\{\{([^}]*?)\}\}/g
  line = line.replace(mustacheRegex, (match, inner) => {
    let newInner = inner
    for (const [cn, key] of sortedEntries) {
      const esc = cn.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      // 替换 '中文' 或 "中文" 为 $t('key')
      newInner = newInner.replace(new RegExp(`['"\`]${esc}['"\`]`, 'g'), `$t('${key}')`)
    }
    return `{{${newInner}}}`
  })
  return line
}

/**
 * 替换拼接字符串中的中文
 * 'xxx' + '中文' + 'yyy' → 'xxx' + $t('key') + 'yyy'
 */
function replaceConcatChinese(line) {
  for (const [cn, key] of sortedEntries) {
    const esc = cn.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    // 匹配 + '中文' + 或 + '中文') 等模式
    line = line.replace(new RegExp(`\\+\\s*['"\`]${esc}['"\`]\\s*\\+`, 'g'), `+ $t('${key}') +`)
    line = line.replace(new RegExp(`\\+\\s*['"\`]${esc}['"\`]\\s*\\)`, 'g'), `+ $t('${key}'))`)
    line = line.replace(new RegExp(`\\(\\s*['"\`]${esc}['"\`]\\s*\\+`, 'g'), `($t('${key}') +`)
  }
  return line
}

// 预编译正则：构建一个匹配所有映射中文的大正则
const cnPattern = sortedEntries.map(([cn]) => cn.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|')
const cnRegexGlobal = new RegExp(`['"\`](${cnPattern})['"\`]`, 'g')

// 快速查找表
const cnLookup = {}
for (const [cn, key] of sortedEntries) {
  cnLookup[cn] = key
}

/**
 * 在 script 上下文中替换 - 使用单次遍历
 */
function replaceScriptLine(line) {
  // 匹配 '中文' 或 "中文" 模式，检查前面是否有特定前缀
  const scriptPattern = /(?:title|message|description|content|label|text|templateName)\s*[:=]\s*(['"`])([^'"`]*[一-鿿][^'"`]*)\1/g
  line = line.replace(scriptPattern, (match, quote, value) => {
    const key = cnLookup[value]
    if (key) {
      // 根据前面的关键词决定用 this.$t 还是 $t
      const prefix = match.match(/^(.*?)[:=]/)[1].trim()
      if (prefix === 'title' || prefix === 'message' || prefix === 'description' || prefix === 'content' || prefix === 'templateName') {
        return match.replace(`${quote}${value}${quote}`, `this.$t('${key}')`)
      }
    }
    return match
  })

  // this.$message.xxx('中文') 模式
  line = line.replace(/(\.\$message\w*\.\w+\()\s*(['"`])([^'"`]*[一-鿿][^'"`]*)\2\s*\)/g, (match, prefix, quote, value) => {
    const key = cnLookup[value]
    if (key) return `${prefix}this.$t('${key}'))`
    return match
  })

  // err.message = '中文' 模式
  line = line.replace(/(err\.message\s*=\s*)(['"`])([^'"`]*[一-鿿][^'"`]*)\2/g, (match, prefix, quote, value) => {
    const key = cnLookup[value]
    if (key) return `${prefix}this.$t('${key}')`
    return match
  })

  // this.$refs.xxx.title = '中文' 模式
  line = line.replace(/(\.title\s*=\s*)(['"`])([^'"`]*[一-鿿][^'"`]*)\2/g, (match, prefix, quote, value) => {
    const key = cnLookup[value]
    if (key) return `${prefix}this.$t('${key}')`
    return match
  })

  return line
}

/**
 * 处理单个文件
 */
function processFile(filePath) {
  const basename = path.basename(filePath)
  if (SKIP_FILES.includes(basename)) return

  const content = fs.readFileSync(filePath, 'utf-8')
  const lines = content.split('\n')
  let changed = false
  let count = 0

  // 跟踪块注释状态
  let inBlockComment = false

  const newLines = lines.map((line) => {
    // 处理块注释状态
    if (line.includes('/*')) inBlockComment = true
    const wasInBlock = inBlockComment
    if (line.includes('*/')) inBlockComment = false

    // 跳过纯注释行
    if (wasInBlock || isCommentLine(line, false)) {
      return line
    }

    // 分离行内注释：只处理代码部分，保留注释部分
    let codePart = line
    let commentPart = ''
    // 匹配不在字符串内的 // 注释
    const inlineCommentMatch = line.match(/^([\s\S]*?)(\/\/[^\n]*)$/)
    if (inlineCommentMatch && !line.trim().startsWith('//')) {
      codePart = inlineCommentMatch[1]
      commentPart = inlineCommentMatch[2]
    }
    // 匹配 HTML 行内注释
    const htmlCommentMatch = codePart.match(/^([\s\S]*?)(<!--[^\n]*-->)$/)
    if (htmlCommentMatch) {
      codePart = htmlCommentMatch[1]
      commentPart = htmlCommentMatch[2] + commentPart
    }

    const original = line

    // 模板替换 (只处理代码部分)
    let result = replaceTemplateAttrs(codePart)
    result = replaceTagContent(result)
    result = replaceInlineText(result)
    result = replaceMustacheChinese(result)
    result = replaceConcatChinese(result)

    // Script 替换：模板标签属性已经由上面的模板规则处理，不能再按
    // JavaScript 的 `title = ...` 规则替换，否则会生成无效的
    // `title=this.$t(...)` 属性。
    if (!codePart.includes('<')) {
      result = replaceScriptLine(result)
    }

    // 重新组装代码和注释
    if (commentPart) {
      result = result + commentPart
    }

    if (result !== original) {
      changed = true
      // 统计替换数量
      for (const [cn] of sortedEntries) {
        const esc = cn.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
        const before = (original.match(new RegExp(esc, 'g')) || []).length
        const after = (result.match(new RegExp(esc, 'g')) || []).length
        count += Math.max(0, before - after)
      }
    }

    return result
  })

  if (changed) {
    totalReplacements += count
    modifiedFiles.push({ file: path.relative(SRC_DIR, filePath), count })
    if (!DRY_RUN) {
      fs.writeFileSync(filePath, newLines.join('\n'), 'utf-8')
    }
  }
}

/**
 * 递归遍历目录
 */
function walkDir(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true })
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      if (!['node_modules', '.git', 'dist', 'scripts'].includes(entry.name)) {
        walkDir(fullPath)
      }
    } else if (entry.name.endsWith('.vue') || entry.name.endsWith('.js')) {
      processFile(fullPath)
    }
  }
}

// 执行
const DRY_RUN = process.argv.includes('--dry-run')

console.log('🔍 扫描目录:', SRC_DIR)
console.log('📝 映射条目:', sortedEntries.length)
if (DRY_RUN) console.log('⚠️  DRY RUN 模式 - 不会修改文件')
console.log('')

walkDir(SRC_DIR)

console.log('')
console.log('✅ 完成!')
console.log(`📊 总替换: ${totalReplacements} 处`)
console.log(`📁 修改文件: ${modifiedFiles.length} 个`)
console.log('')
if (modifiedFiles.length > 0) {
  console.log('修改的文件:')
  modifiedFiles.sort((a, b) => b.count - a.count).forEach(f => {
    console.log(`  ${f.file}: ${f.count} 处`)
  })
}
