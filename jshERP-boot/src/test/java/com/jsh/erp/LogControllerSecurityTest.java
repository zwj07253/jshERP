package com.jsh.erp;

import com.jsh.erp.controller.LogController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class LogControllerSecurityTest {
    @Test
    void exposesNoAuditMutationEndpoints() {
        Method[] methods = LogController.class.getDeclaredMethods();
        assertFalse(Arrays.stream(methods).anyMatch(method ->
                method.isAnnotationPresent(PostMapping.class)
                        || method.isAnnotationPresent(PutMapping.class)
                        || method.isAnnotationPresent(DeleteMapping.class)));
    }
}
