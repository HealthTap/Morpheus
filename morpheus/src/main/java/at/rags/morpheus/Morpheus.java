package at.rags.morpheus;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * Morpheus is a library to map JSON with the json:api specification format.
 * (http://jsonapi.org/).
 *
 * Feel free to contribute on github. (//TODO insert new link here)
 *
 * Example
 * <pre>
 * {@code
 *  Morpheus morpheus = new Morpheus();
 *  JsonApiObject jsonapiObject = morpheus.parse(YOUR-JSON-STRING);
 * }
 * </pre>
 */
public class Morpheus {
  private Mapper mapper;

  public Morpheus() {
    mapper = new Mapper();
  }

  public Morpheus(AttributeMapper attributeMapper) {
    mapper = new Mapper(new Deserializer(), new Serializer(), attributeMapper);
    Factory.setMapper(mapper);
  }

  /**
   * Will return you an {@link JsonApiObject} with parsed objects, links, relations and includes.
   *
   * @param jsonString Your json:api formated string.
   * @return A {@link JsonApiObject}.
   * @throws JSONException or NotExtendingResourceException
   */
  public JsonApiObject parse(String jsonString) throws Exception {
    JSONObject jsonObject = null;
    try {
      jsonObject = new JSONObject(jsonString);
    } catch (JSONException e) {
      throw e;
    }

    return parseFromJSONObject(jsonObject);
  }

  /**
   * Parse and map all the top level members.
   */
  private JsonApiObject parseFromJSONObject(JSONObject jsonObject) throws Exception {
    JsonApiObject jsonApiObject = new JsonApiObject();

    //included
    try {
      JSONArray includedArray = jsonObject.getJSONArray("included");
      jsonApiObject.setIncluded(Factory.newObjectFromJSONArray(includedArray, null));
    } catch (JSONException e) {
      Logger.debug("JSON does not contain included");
    }

    //data array
    JSONArray dataArray = null;
    try {
      dataArray = jsonObject.getJSONArray("data");
      jsonApiObject.setResources(Factory.newObjectFromJSONArray(dataArray, jsonApiObject.getIncluded()));
    } catch (JSONException e) {
      Logger.debug("JSON does not contain data array");
    }

    //data object
    JSONObject dataObject = null;
    try {
      dataObject = jsonObject.getJSONObject("data");
      jsonApiObject.setResource(Factory.newObjectFromJSONObject(dataObject, jsonApiObject.getIncluded()));
    } catch (JSONException e) {
      Logger.debug("JSON does not contain data object");
    }

    //link object
    JSONObject linkObject = null;
    try {
      linkObject = jsonObject.getJSONObject("links");
      jsonApiObject.setLinks(mapper.mapLinks(linkObject));
    } catch (JSONException e) {
      Logger.debug("JSON does not contain links object");
    }

    //meta object
    JSONObject metaObject = null;
    try {
      metaObject = jsonObject.getJSONObject("meta");
      jsonApiObject.setMeta(mapper.getAttributeMapper().createMapFromJSONObject(metaObject));
    } catch (JSONException e) {
      Logger.debug("JSON does not contain meta object");
    }

    JSONArray errorArray = null;
    try {
      errorArray = jsonObject.getJSONArray("errors");
      jsonApiObject.setErrors(mapper.mapErrors(errorArray));
    } catch (JSONException e) {
      Logger.debug("JSON does not contain errors object");
    }

    return jsonApiObject;
  }

  public String createJson(JsonApiObject jsonApiObject) {
    HashMap<String, Object> jsonMap = new HashMap<>();

    if (jsonApiObject.getResource() != null) {
      HashMap<String, Object> data = mapper.createDataFromJsonResource(jsonApiObject.getResource(), true);
      if (data != null) {
        jsonMap.put("data", data);
      }
    }

    if (jsonApiObject.getResources() != null) {
      ArrayList<HashMap<String, Object>> data = mapper.createDataFromJsonResources(jsonApiObject.getResources(), true);
      if (data != null) {
        jsonMap.put("data", data);
      }
    }

    Gson gson = new Gson();
    return gson.toJson(jsonMap);
  }
}
