package io.github.multiapples;

import com.google.gson.*;

public class JsonOption<T extends JsonElement> {
    private final T value;

    public JsonOption (T element) {
        value = element;
    }

    public boolean elementExists() {
        return value != null;
    }

    public boolean nonNull() {
        return this.elementExists() && !value.isJsonNull();
    }

    public JsonOption<JsonElement> get(String memberName) {
        if (value != null && value.isJsonObject()) {
            return new JsonOption<>(((JsonObject)value).get(memberName));
        }
        return new JsonOption<>(null);
    }

    public JsonOption<JsonElement> get(int index) {
        if (value != null && value.isJsonArray()) {
            return new JsonOption<>(((JsonArray)value).get(index));
        }
        return new JsonOption<>(null);
    }

    public JsonObject unwrapAsJsonObject() throws IllegalStateException {
        try {
            return value.getAsJsonObject();
        } catch (NullPointerException | IllegalStateException e) {
            throw new IllegalStateException();
        }
    }

    public JsonObject unwrapAsJsonObject(JsonObject defaultValue) {
        try {
            return value.getAsJsonObject();
        } catch (NullPointerException | IllegalStateException e) {
            return defaultValue;
        }
    }

    public JsonArray unwrapAsJsonArray() throws IllegalStateException {
        try {
            return value.getAsJsonArray();
        } catch (NullPointerException | IllegalStateException e) {
            throw new IllegalStateException();
        }
    }

    public JsonArray unwrapAsJsonArray(JsonArray defaultValue) {
        try {
            return value.getAsJsonArray();
        } catch (NullPointerException | IllegalStateException e) {
            return defaultValue;
        }
    }

    public Number unwrapAsNumber() throws IllegalStateException {
        try {
            return value.getAsJsonPrimitive().getAsNumber();
        } catch (NullPointerException | IllegalStateException | UnsupportedOperationException e) {
            throw new IllegalStateException();
        }
    }

    public Number unwrapAsNumber(Number defaultValue) {
        try {
            return value.getAsJsonPrimitive().getAsNumber();
        } catch (NullPointerException | IllegalStateException | UnsupportedOperationException e) {
            return defaultValue;
        }
    }

    public String unwrapAsString() throws IllegalStateException {
        try {
            return value.getAsJsonPrimitive().getAsString();
        } catch (NullPointerException | IllegalStateException e) {
            throw new IllegalStateException();
        }
    }

    public String unwrapAsString(String defaultValue) {
        try {
            return value.getAsJsonPrimitive().getAsString();
        } catch (NullPointerException | IllegalStateException e) {
            return defaultValue;
        }
    }
}
