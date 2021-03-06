package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.DevModeHandlerTest;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.NodeUpdater;

import elemental.json.Json;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_REUSE_DEV_SERVER;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.createStubNode;
import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.createStubWebpackServer;

/**
 * Base class for DevModeInitializer tests. It is an independent class so as it
 * can be created and executed with custom classloaders.
 */
public class DevModeInitializerTestBase {

    DevModeInitializer devModeInitializer;

    // These fields are intentionally scoped default so
    // as they can be used in package tests
    ServletContext servletContext;
    Map<String, String> initParams;
    Set<Class<?>> classes;
    File mainPackageFile;
    File webpackFile;
    String baseDir;

    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setup() throws Exception {
        temporaryFolder.create();
        baseDir = temporaryFolder.getRoot().getPath();

        createStubNode(false, true, baseDir);
        createStubWebpackServer("Compiled", 0, baseDir);

        servletContext = Mockito.mock(ServletContext.class);
        ServletRegistration registration = Mockito
                .mock(ServletRegistration.class);

        initParams = new HashMap<>();
        initParams.put(FrontendUtils.PROJECT_BASEDIR, baseDir);
        initParams.put(Constants.SERVLET_PARAMETER_DISABLE_PNPM, "true");

        Mockito.when(registration.getInitParameters()).thenReturn(initParams);

        classes = new HashSet<>();
        classes.add(this.getClass());

        Map registry = new HashMap();
        registry.put("foo", registration);
        Mockito.when(servletContext.getServletRegistrations())
                .thenReturn(registry);
        Mockito.when(servletContext.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(servletContext.getClassLoader())
                .thenReturn(this.getClass().getClassLoader());

        mainPackageFile = new File(baseDir, PACKAGE_JSON);
        webpackFile = new File(baseDir, WEBPACK_CONFIG);

        // Not this needs to update according to dependencies in
        // NodeUpdater.getDefaultDependencies and NodeUpdater.getDefaultDevDependencies
        FileUtils.write(mainPackageFile, getInitalPackageJson().toJson(),
                "UTF-8");
        webpackFile.createNewFile();

        // Default is Bower Mode, change to Npm Mode
        System.setProperty("vaadin." + SERVLET_PARAMETER_COMPATIBILITY_MODE,
                "false");

        devModeInitializer = new DevModeInitializer();
    }

    private JsonObject getInitalPackageJson() {
        JsonObject packageJson = Json.createObject();
        JsonObject vaadinPackages = Json.createObject();

        vaadinPackages.put("dependencies", Json.createObject());
        JsonObject defaults = vaadinPackages.getObject("dependencies");
        defaults.put("@polymer/polymer", "3.2.0");
        defaults.put("@webcomponents/webcomponentsjs", "^2.2.10");

        vaadinPackages.put("devDependencies", Json.createObject());
        defaults = vaadinPackages.getObject("devDependencies");
        defaults.put("webpack", "4.30.0");
        defaults.put("webpack-cli", "3.3.0");
        defaults.put("webpack-dev-server", "3.3.0");
        defaults.put("webpack-babel-multi-target-plugin", "2.3.1");
        defaults.put("copy-webpack-plugin", "5.0.3");
        defaults.put("compression-webpack-plugin", "3.0.0");
        defaults.put("webpack-merge", "4.2.1");
        defaults.put("raw-loader", "3.0.0");

        vaadinPackages.put("hash", "");

        packageJson.put("vaadin", vaadinPackages);

        return packageJson;
    }

    @After
    public void teardown() throws Exception, SecurityException {
        System.clearProperty("vaadin." + SERVLET_PARAMETER_COMPATIBILITY_MODE);
        System.clearProperty("vaadin." + SERVLET_PARAMETER_PRODUCTION_MODE);
        System.clearProperty("vaadin." + SERVLET_PARAMETER_REUSE_DEV_SERVER);

        webpackFile.delete();
        mainPackageFile.delete();
        temporaryFolder.delete();
        if (DevModeHandler.getDevModeHandler() != null) {
            DevModeHandler.getDevModeHandler().removeRunningDevServerPort();
        }
        DevModeHandlerTest.removeDevModeHandlerInstance();
    }

    public void runOnStartup() throws Exception {
        devModeInitializer.onStartup(classes, servletContext);
    }

    public void runDestroy() throws Exception {
        devModeInitializer.contextDestroyed(null);
    }

    static List<URL> getClasspathURLs() {
        return Arrays.stream(
                System.getProperty("java.class.path").split(File.pathSeparator))
                .map(s -> {
                    try {
                        return new File(s).toURI().toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }

    public DevModeHandler getDevModeHandler() {
        return DevModeHandler.getDevModeHandler();
    }
}
