package at.rags.morpheus;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.SerializedName;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

import at.rags.morpheus.annotations.JsonApiType;

/**
 * Extend this resource to your custom Object you want to map.
 * You can set custom json object names and relationships via the provided annotations.
 * <pre>
 * {@code
 * public class Article extends Resource { ... }
 * }</pre>
 *
 * @see com.google.gson.annotations.SerializedName
 * @see at.rags.morpheus.annotations.Relationship
 */
public class Resource {
    private String id;
    private at.rags.morpheus.Links links;
    private HashMap<String, Object> meta;

    private ArrayList<String> nullableRelationships = new ArrayList<>();

    public Resource() {
    }

    public HashMap<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(HashMap<String, Object> meta) {
        this.meta = meta;
    }

    public at.rags.morpheus.Links getLinks() {
        return links;
    }

    public void setLinks(Links links) {
        this.links = links;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<String> getNullableRelationships() {
        return nullableRelationships;
    }

    public void resetNullableRelationships() {
        nullableRelationships.clear();
    }

    /**
     * Add here your relationship name, if you want to null it while serializing.
     * This can be used to remove relationships from your object.
     *
     * @param relationshipName Name of your relationship.
     */
    public void addRelationshipToNull(String relationshipName) {
        if (relationshipName == null) {
            return;
        }

        nullableRelationships.add(relationshipName);
    }

    public static class ResourceSerializer<T> implements JsonSerializer<T> {

        @Override
        public JsonObject serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            if (src instanceof Resource) {
                Resource resource = (Resource) src;
                jsonObject.addProperty("id", resource.getId());
                JsonApiType type = src.getClass().getAnnotation(JsonApiType.class);
                if (type != null) {
                    jsonObject.addProperty("type", type.value());
                }
            }
            Field[] fields = src.getClass().getDeclaredFields();
//            Log.d("JSONApi", "class:" + src.getClass());
            for (Field field : fields) {
                SerializedName serializedName = field.getAnnotation(SerializedName.class);
                if (serializedName == null) {
                    continue;
                }
//                Log.d("JSONApi", "SerializedName:" + serializedName.value());
//                Log.d("JSONApi", "Type:" + field.getType());
                boolean accessible = field.isAccessible();
                field.setAccessible(true);
                try {
                    if (int.class == field.getType()) {
                        jsonObject.addProperty(serializedName.value(), field.getInt(src));
                    } else if (long.class.equals(field.getType())) {
                        jsonObject.addProperty(serializedName.value(), field.getLong(src));
                    } else if (float.class.isAssignableFrom(field.getType())) {
                        jsonObject.addProperty(serializedName.value(), field.getFloat(src));
                    } else if (double.class.isAssignableFrom(field.getType())) {
                        jsonObject.addProperty(serializedName.value(), field.getDouble(src));
                    } else if (boolean.class.isAssignableFrom(field.getType())) {
                        jsonObject.addProperty(serializedName.value(), field.getBoolean(src));
                    } else if (String.class.equals(field.getType())) {
                        jsonObject.addProperty(serializedName.value(), field.get(src).toString());
                    } else {
                        jsonObject.add(serializedName.value(), context.serialize(field.get(src)));
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } finally {
                    field.setAccessible(accessible);
                }
            }
            return jsonObject;
        }
    }
}

