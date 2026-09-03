package com.matheus.procurement.execution;

import com.matheus.procurement.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class ExecutionEngineTest  {

    @Autowired
    private ExecutionEngine executionEngine;

    @Test
    void shouldRunAsynchronously() throws Exception {
        Task task = new Task();
        task.setType("summarize");
        task.setPayload("test payload");

        String testThreadName = Thread.currentThread().getName();
        CompletableFuture<String> result = executionEngine.execute(task);

        String resultTest = result.get();

        assertThat(resultTest).isNotEqualTo(testThreadName);
    }
}
