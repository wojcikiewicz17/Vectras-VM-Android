package com.vectras.vm.core;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Funções puras para parse/validação de lista de VMs serializadas em JSON.
 */
public final class VmJsonParser {

    private VmJsonParser() {
    }

    public static ArrayList<HashMap<String, Object>> parseVmListJson(String json, String logTag) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ArrayList<HashMap<String, Object>> vmList = new Gson().fromJson(json,
                    new TypeToken<ArrayList<HashMap<String, Object>>>() {
                    }.getType());
            return vmList != null ? vmList : new ArrayList<>();
        } catch (RuntimeException parseError) {
            Log.w(logTag, "parseVmListJson: invalid JSON, using empty list", parseError);
            return new ArrayList<>();
        }
    }

    public static boolean isValidVmPosition(ArrayList<HashMap<String, Object>> vmList, int position) {
        return vmList != null && position >= 0 && position < vmList.size();
    }
}
