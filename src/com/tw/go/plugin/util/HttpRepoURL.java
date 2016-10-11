package com.tw.go.plugin.util;

import com.squareup.okhttp.*;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.params.CoreConnectionPNames;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class HttpRepoURL extends RepoUrl {


    public HttpRepoURL(String url, String user, String password) {
        super(url, user, password);
    }

    public static OkHttpClient getHttpClient() {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(5, TimeUnit.SECONDS);
        //TODO: OKHttpClient doesn't offer configurable retry attempts. Do we extend it to retry 3 times (as before) or use default retry behavior?
        //TODO: Used to configure redirect via the Apache DefaultRedirectStrategy(). Need to update handling of redirect using OkHttpClient.
        client.setRetryOnConnectionFailure(true);
        return client;
    }

    public void validate(ValidationResult validationResult) {
        try {
            doBasicValidations(validationResult);
            URL validatedUrl = new URL(this.url);
            if (!(validatedUrl.getProtocol().startsWith("http"))) {
                validationResult.addError(new ValidationError(REPO_URL, "Invalid URL: Only http is supported."));
            }

            if (StringUtil.isNotBlank(validatedUrl.getUserInfo())) {
                validationResult.addError(new ValidationError(REPO_URL, "User info should not be provided as part of the URL. Please provide credentials using USERNAME and PASSWORD configuration keys."));
            }
            credentials.validate(validationResult);
        } catch (MalformedURLException e) {
            validationResult.addError(new ValidationError(REPO_URL, "Invalid URL : " + url));
        }
    }


    public void checkConnection(String urlOverride) {
        OkHttpClient client = getHttpClient();
        Request request;
        if (credentials.provided()) {
            String usernamePasswordCredentials = com.squareup.okhttp.Credentials.basic(credentials.getUser(),credentials.getPassword());
            request = new Request.Builder()
                    .url((urlOverride == null) ? url : urlOverride)
                    .header("Authorization", usernamePasswordCredentials)
                    .build();
        } else {
            request = new Request.Builder()
                    .url((urlOverride == null) ? url : urlOverride)
                    .build();
        }
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new RuntimeException("Unexpected code " + response.code());
            } else {
                ResponseBody body = response.body();
                System.out.println(body.string());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUrlWithBasicAuth() {
        return getUrlWithCreds(url, credentials);
    }

    public static String getUrlWithCreds(String urlStr, Credentials credentials) {
        try {
            URL urlObj = new URL(urlStr);
            StringBuilder sb = new StringBuilder();
            sb.append(urlObj.getProtocol());
            sb.append("://");
            if (credentials.provided()) {
                sb.append(credentials.getUserInfo()).append("@");
            }
            sb.append(urlObj.getHost());
            if(urlObj.getPort() != -1){
                sb.append(":").append(urlObj.getPort());
            }
            sb.append(urlObj.getPath());
            if(urlObj.getQuery() != null)
                sb.append("?").append(urlObj.getQuery());
            if(urlObj.getRef() != null)
                sb.append("#").append(urlObj.getRef());
            if(urlObj.getQuery() == null && urlObj.getRef() == null && !urlObj.getPath().endsWith("/"))
                sb.append("/");
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getUrlStrWithTrailingSlash() {
        String urlStr = getUrlStr();
        if (urlStr.endsWith("/")) return urlStr;
        return urlStr + "/";
    }
}
