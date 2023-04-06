package com.leo.flink.http.connector.util;

import com.google.common.collect.Lists;
import com.google.gson.*;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import static com.google.gson.stream.JsonToken.NULL;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

/**
 * @author xuefengyu
 */
public class JSONObjectTools {
    private static final TypeAdapterFactory STRING_FACTORY = new TypeAdapterFactory() {
        @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            return typeToken.getRawType() == String.class ? (TypeAdapter<T>) STRING : null;
        }

        @Override
        public String toString() {
            return "Factory[type=String,adapter=" + STRING.getClass().getName() + "]";
        }
    };
    private static final TypeAdapter<String> STRING = new TypeAdapter<String>() {
        @Override
        public String read(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            if (peek == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            /* coerce booleans to strings for backwards compatibility */
            if (peek == JsonToken.BOOLEAN) {
                return Boolean.toString(in.nextBoolean());
            }
            return in.nextString();
        }

        @Override
        public void write(JsonWriter out, String value) throws IOException {
            if (value == null) {
                out.value("");
            } else {
                out.value(value);
            }
        }
    };

    private static final Gson snakeJson = new GsonBuilder()
            .setFieldNamingStrategy(field -> FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES.translateName(field))
            .registerTypeAdapterFactory(STRING_FACTORY).create();

    private static final Gson listAsEmptyJson =
            new GsonBuilder().serializeNulls().registerTypeAdapterFactory(new ListTypeAdapterFactory())
                    .registerTypeAdapterFactory(STRING_FACTORY).setDateFormat("yyyy-MM-dd HH:mm:ss").
                    create();

    private static final Gson withNullJson =
            new GsonBuilder().serializeNulls().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    private static final Gson listAsEmptyAndPrettyJson =
            new GsonBuilder().serializeNulls().registerTypeAdapterFactory(new ListTypeAdapterFactory())
                    .registerTypeAdapterFactory(STRING_FACTORY).setDateFormat("yyyy-MM-dd HH:mm:ss").setPrettyPrinting().
                    create();

    private static final Gson noNullAndPrettyJson =
            new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").setPrettyPrinting().
                    create();

    private static final Gson simpleJson = new GsonBuilder().create();

    public static String toSnakeJson(Object bean) {
        return snakeJson.toJson(bean);
    }

    /**
     * 获取原始的json串
     *
     * @param bean
     * @return
     */
    public static String toOriJsonString(Object bean) {
        return listAsEmptyJson.toJson(bean);
    }

    /**
     * 获取格式化json串
     *
     * @param bean
     * @return
     */
    public static String toFormatJsonString(Object bean) {
        return listAsEmptyAndPrettyJson.toJson(bean);
    }

    /**
     * 获取原始的json串 NoNull
     *
     * @param bean
     * @return
     */
    public static String toOriNoNullJsonString(Object bean) {
        return withNullJson.toJson(bean);
    }

    /**
     * 获取原始的json串
     *
     * @param bean
     * @return
     */
    public static String toFormatNoNullJsonString(Object bean) {
        return noNullAndPrettyJson.toJson(bean);
    }

    /**
     * 用fastjson 将json字符串解析为一个 JavaBean
     *
     * @param jsonString
     * @param cls
     * @return
     */
    public static <T> T jsonToBean(String jsonString, Class<T> cls) {
        return simpleJson.fromJson(jsonString, cls);
    }

    /**
     * 用fastjson 将json字符串 解析成为一个 List<JavaBean> 及 List<String>
     *
     * @param jsonString
     * @return
     */
    public static <T> List<T> jsonToList(String jsonString, Class<T> clazz) {
        return simpleJson.fromJson(jsonString, new ListOfJson<T>(clazz));
    }

    /**
     * 用fastjson 将jsonString 解析成 List<Map<String,Object>>
     *
     * @param jsonString
     * @return
     */
    public static Map<String, Object> jsonToMap(String jsonString) {
        return simpleJson.fromJson(jsonString, Map.class);
    }

    private static class ListOfJson<T> implements ParameterizedType {
        private Class<?> wrapped;

        public ListOfJson(Class<T> wrapper) {
            this.wrapped = wrapper;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{wrapped};
        }

        @Override
        public Type getRawType() {
            return List.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

    private static class ListTypeAdapterFactory implements TypeAdapterFactory {

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            Type type = typeToken.getType();

            Class<? super T> rawType = typeToken.getRawType();
            if (!List.class.isAssignableFrom(rawType)) {
                return null;
            }

            Type elementType = $Gson$Types.getCollectionElementType(type, rawType);
            TypeAdapter<?> elementTypeAdapter = gson.getAdapter(TypeToken.get(elementType));

            @SuppressWarnings({"unchecked", "rawtypes"}) // create() doesn't define a type parameter
                    TypeAdapter<T> result = new Adapter(gson, elementType, elementTypeAdapter);
            return result;
        }

        private static final class Adapter<E> extends TypeAdapter<List<E>> {
            private final TypeAdapter<E> elementTypeAdapter;

            public Adapter(Gson context, Type elementType, TypeAdapter<E> elementTypeAdapter) {
                this.elementTypeAdapter =
                        new TypeAdapterRuntimeTypeWrapper<E>(context, elementTypeAdapter, elementType);
            }

            @Override
            public List<E> read(JsonReader in) throws IOException {
                if (in.peek() == NULL) {
                    in.nextNull();
                    return null;
                }

                List<E> collection = Lists.newArrayList();
                in.beginArray();
                while (in.hasNext()) {
                    E instance = elementTypeAdapter.read(in);
                    collection.add(instance);
                }
                in.endArray();
                return collection;
            }

            @Override
            public void write(JsonWriter out, List<E> collection) throws IOException {
                if (collection == null) {
                    out.beginArray().endArray();
                    return;
                }

                out.beginArray();
                for (E element : collection) {
                    elementTypeAdapter.write(out, element);
                }
                out.endArray();
            }
        }
    }

    private static class TypeAdapterRuntimeTypeWrapper<T> extends TypeAdapter<T> {
        private final Gson context;
        private final TypeAdapter<T> delegate;
        private final Type type;

        TypeAdapterRuntimeTypeWrapper(Gson context, TypeAdapter<T> delegate, Type type) {
            this.context = context;
            this.delegate = delegate;
            this.type = type;
        }

        @Override
        public T read(JsonReader in) throws IOException {
            return delegate.read(in);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public void write(JsonWriter out, T value) throws IOException {
            // Order of preference for choosing type adapters
            // First preference: a type adapter registered for the runtime type
            // Second preference: a type adapter registered for the declared type
            // Third preference: reflective type adapter for the runtime type (if it is a sub class of the declared type)
            // Fourth preference: reflective type adapter for the declared type

            TypeAdapter chosen = delegate;
            Type runtimeType = getRuntimeTypeIfMoreSpecific(type, value);
            if (runtimeType != type) {
                TypeAdapter runtimeTypeAdapter = context.getAdapter(TypeToken.get(runtimeType));
                if (!(runtimeTypeAdapter instanceof ReflectiveTypeAdapterFactory.Adapter)) {
                    // The user registered a type adapter for the runtime type, so we will use that
                    chosen = runtimeTypeAdapter;
                } else if (!(delegate instanceof ReflectiveTypeAdapterFactory.Adapter)) {
                    // The user registered a type adapter for Base class, so we prefer it over the
                    // reflective type adapter for the runtime type
                    chosen = delegate;
                } else {
                    // Use the type adapter for runtime type
                    chosen = runtimeTypeAdapter;
                }
            }
            chosen.write(out, value);
        }

        /**
         * Finds a compatible runtime type if it is more specific
         */
        private Type getRuntimeTypeIfMoreSpecific(Type type, Object value) {
            if (value != null && (type == Object.class || type instanceof TypeVariable<?>
                    || type instanceof Class<?>)) {
                type = value.getClass();
            }
            return type;
        }
    }

    public static void main(String[] args){
        byte[] bytes={'a','c'};
        System.out.println(JSONObjectTools.toFormatJsonString(null));
    }

}
