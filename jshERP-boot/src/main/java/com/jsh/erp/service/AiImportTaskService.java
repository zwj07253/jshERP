package com.jsh.erp.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.datasource.entities.User;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.UUID;

/** Short-lived, user-bound AI import preview sessions. */
@Service
public class AiImportTaskService {
    private static final long TASK_TTL_SECONDS = 15 * 60L;
    private static final String TASK_PREFIX = "ai:import:task:";
    private static final String LOCK_PREFIX = "ai:import:confirm:";

    @Resource private RedisService redisService;
    @Resource private UserService userService;

    public String create(String type, String prefixNo, JSONArray rows, JSONArray warnings) throws Exception {
        User user = userService.getCurrentUser();
        String taskId = UUID.randomUUID().toString().replace("-", "");
        JSONObject task = new JSONObject();
        task.put("userId", user.getId());
        task.put("tenantId", user.getTenantId());
        task.put("type", type);
        task.put("prefixNo", prefixNo == null ? "" : prefixNo);
        task.put("rows", rows);
        task.put("warnings", warnings == null ? new JSONArray() : warnings);
        redisService.storageKeyWithTime(TASK_PREFIX + taskId, task.toJSONString(), TASK_TTL_SECONDS);
        return taskId;
    }

    public JSONObject require(String taskId, String type, String prefixNo) throws Exception {
        if (taskId == null || taskId.trim().isEmpty()) throw new IllegalArgumentException("AI 导入任务不存在，请重新识别文件");
        String raw = redisService.getCacheObject(TASK_PREFIX + taskId);
        if (raw == null || raw.trim().isEmpty()) throw new IllegalArgumentException("AI 导入任务已过期，请重新识别文件");
        JSONObject task = JSON.parseObject(raw);
        User user = userService.getCurrentUser();
        if (!user.getId().equals(task.getLong("userId")) || !equals(user.getTenantId(), task.getLong("tenantId"))) {
            throw new SecurityException("无权确认其他用户的 AI 导入任务");
        }
        if (!type.equals(task.getString("type")) || !equals(prefixNo, task.getString("prefixNo"))) {
            throw new IllegalArgumentException("AI 导入任务与当前导入类型不一致，请重新识别文件");
        }
        return task;
    }

    public boolean lock(String taskId) { return redisService.setIfAbsent(LOCK_PREFIX + taskId, "1", TASK_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS); }
    public void unlock(String taskId) { redisService.deleteObject(LOCK_PREFIX + taskId); }
    private boolean equals(Object a, Object b) { return a == null ? b == null : a.equals(b); }
}
