package at.rags.morpheus.retrofit;

import android.util.Log;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import at.rags.morpheus.Deserializer;
import at.rags.morpheus.Morpheus;
import at.rags.morpheus.Resource;
import at.rags.morpheus.annotations.JsonApiType;
import at.rags.morpheus.exceptions.TypeNameMissingException;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Created by wuhaoouyang on 3/2/17.
 */

public class JsonApiConverterFactory extends Converter.Factory {

    private Morpheus morpheus;

    public static at.rags.morpheus.retrofit.JsonApiConverterFactory create() {
        return new at.rags.morpheus.retrofit.JsonApiConverterFactory();
    }

    public static at.rags.morpheus.retrofit.JsonApiConverterFactory create(Class<? extends Resource>... types) {
        if (types != null) {
            for (Class type : types) {
                if (Resource.class.isAssignableFrom(type)) {
                    registerResourceClass(type);
                }
            }
        }
        return new at.rags.morpheus.retrofit.JsonApiConverterFactory();
    }

    private JsonApiConverterFactory() {
        morpheus = new Morpheus();
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        Log.d("JSONApi", "type: " + type + "\nclass: " + type.getClass());
        if (type instanceof Class && Resource.class.isAssignableFrom((Class<?>) type)) {
//            Class<?> typeClass = (Class<?>) type;
//            String typeName = typeClass.getAnnotation(JsonApiType.class).value();
//            Deserializer.registerResourceClass(typeName, typeClass);
//            Field[] fields = typeClass.getFields();
//            if (fields != null) {
//                for (Field field : fields) {
//                    if (Resource.class.isAssignableFrom(field.getType())) {
//                        registerResourceClass(field.getType());
//                    }
//                }
//            }
            Log.d("JSONApi", "JSONApi Resource" + type);
            return new JsonApiResponseConverter<>(morpheus, (Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Log.d("JSONApi", "raw: " + parameterizedType.getRawType());
            Log.d("JSONApi", "parameterized: " + parameterizedType.getActualTypeArguments()[0]);
            if (parameterizedType.getRawType() == List.class
                && Resource.class.isAssignableFrom((Class<?>) parameterizedType.getActualTypeArguments()[0])) {
                Log.d("JSONApi", "JSONApi list Resource: " + type);
                return new JsonApiResponseConverter<>(morpheus, (Class<?>) parameterizedType.getRawType());
            }
        }
        return null;
    }

    private static void registerResourceClass(Class<?> type) throws TypeNameMissingException {
        Annotation a = type.getAnnotation(JsonApiType.class);
        if (a == null) {
            throw new TypeNameMissingException(type);
        }
        Deserializer.registerResourceClass(((JsonApiType) a).value(), type);
    }
}
