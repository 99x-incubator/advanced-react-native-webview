package com.advancedwebview;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CustomWebviewPackage implements ReactPackage {
    private CustomWebviewManager manager;
    private CustomWebviewModule module;

    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.emptyList();
    }

    @Override public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        manager = new CustomWebviewManager();
        manager.setPackage(this);
        return Arrays.<ViewManager>asList(manager);
    }

    @Override public List<NativeModule> createNativeModules( ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();
        module = new CustomWebviewModule(reactContext);
        module.setPackage(this);
        modules.add(module);
        return modules;
    }

    public CustomWebviewManager getManager(){
        return manager;
    }

    public CustomWebviewModule getModule(){
        return module;
    }
}