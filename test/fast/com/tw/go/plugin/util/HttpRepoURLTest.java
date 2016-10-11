package com.tw.go.plugin.util;

import org.junit.Test;

import java.net.UnknownHostException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HttpRepoURLTest {
    @Test
    public void shouldNotThrowExceptionIfCheckConnectionToTheRepoPasses() {
        RepoUrl.create("http://google.com", null, null).checkConnection();
    }

    @Test
    public void shouldFailCheckConnectionToTheRepoWhenUrlIsNotReachable() {
        try {
            RepoUrl.create("http://the-internet.herokuapp.com/status_codes/500", null, null).checkConnection();
            fail("should fail");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("500"));
        }
    }
}
