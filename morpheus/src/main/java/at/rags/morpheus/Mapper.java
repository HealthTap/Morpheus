package at.rags.morpheus;

import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import at.rags.morpheus.Annotations.Relationship;
import at.rags.morpheus.Exceptions.NotExtendingResourceException;

/**
 * Mapper will map all different top-level members and will
 * also map the relations.
 *
 * Includes will also mapped to matching relationship members.
 */
public class Mapper {

  private Deserializer deserializer;
  private Serializer serializer;
  private AttributeMapper attributeMapper;

  public Mapper() {
    deserializer = new Deserializer();
    serializer = new Serializer();
    attributeMapper = new AttributeMapper();
  }

  public Mapper(Deserializer deserializer, Serializer serializer, AttributeMapper attributeMapper) {
    this.deserializer = deserializer;
    this.serializer = serializer;
    this.attributeMapper = attributeMapper;
  }

  //TODO map href and meta (http://jsonapi.org/format/#document-links)
  /**
   * Will map links and return them.
   *
   * @param linksJsonObject JSONObject from link.
   * @return Links with mapped values.
   */
  public Links mapLinks(JSONObject linksJsonObject) {
    Links links = new Links();
    try {
      links.setSelfLink(linksJsonObject.getString("self"));
    } catch (JSONException e) {
      Logger.debug("JSON link does not contain self");
    }

    try {
      links.setRelated(linksJsonObject.getString("related"));
    } catch (JSONException e) {
      Logger.debug("JSON link does not contain related");
    }

    try {
      links.setFirst(linksJsonObject.getString("first"));
    } catch (JSONException e) {
      Logger.debug("JSON link does not contain first");
    }

    try {
      links.setLast(linksJsonObject.getString("last"));
    } catch (JSONException e) {
      Logger.debug("JSON link does not contain last");
    }

    try {
      links.setPrev(linksJsonObject.getString("prev"));
    } catch (JSONException e) {
      Logger.debug("JSON link does not contain prev");
    }

    try {
      links.setNext(linksJsonObject.getString("next"));
    } catch (JSONException e) {
      Logger.debug("JSON link does not contain next");
    }

    return links;
  }

  /**
   * Map the Id from json to the object.
   *
   * @param object Object of the class.
   * @param jsonDataObject JSONObject of the dataNode.
   * @return Object with mapped fields.
   * @throws NotExtendingResourceException Throws when the object is not extending {@link Resource}
   */
  public Resource mapId(Resource object, JSONObject jsonDataObject) throws NotExtendingResourceException {
    try {
      return deserializer.setIdField(object, jsonDataObject.get("id"));
    } catch (JSONException e) {
      Logger.debug("JSON data does not contain id.");
    }

    return object;
  }

  /**
   * Maps the attributes of json to the object.
   *
   * @param object Object of the class.
   * @param attributesJsonObject Attributes object inside the data node.
   * @return Object with mapped fields.
   */
  public Resource mapAttributes(Resource object, JSONObject attributesJsonObject) {
    if (attributesJsonObject == null) {
      return object;
    }

    for (Field field : object.getClass().getDeclaredFields()) {
      // get the right attribute name
      String jsonFieldName = field.getName();
      boolean isRelation = false;
      for (Annotation annotation : field.getAnnotations()) {
        if (annotation.annotationType() == SerializedName.class) {
          SerializedName serializeName = (SerializedName) annotation;
          jsonFieldName = serializeName.value();
        }
        if (annotation.annotationType() == Relationship.class) {
          isRelation = true;
        }
      }

      if (isRelation) {
        continue;
      }

      attributeMapper.mapAttributeToObject(object, attributesJsonObject, field, jsonFieldName);
    }

    return object;
  }

  /**
   * Loops through relation JSON array and maps annotated objects.
   *
   * @param object Real object to map.
   * @param jsonObject JSONObject.
   * @param included List of included resources.
   * @return Real object with relations.
   * @throws Exception when deserializer is not able to create instance.
   */
  public Resource mapRelations(Resource object, JSONObject jsonObject,
                                       List<Resource> included) throws Exception {
    HashMap<String, String> relationshipNames = getRelationshipNames(object.getClass());

    //going through relationship names annotated in Class
    for (String relationship : relationshipNames.keySet()) {
      JSONObject relationJsonObject = null;
      try {
        relationJsonObject = jsonObject.getJSONObject(relationship);
      } catch (JSONException e) {
        Logger.debug("Relationship named " + relationship + "not found in JSON");
        continue;
      }

      //map json object of data
      JSONObject relationDataObject = null;
      try {
        relationDataObject = relationJsonObject.getJSONObject("data");
        Resource relationObject = Factory.newObjectFromJSONObject(relationDataObject, null);

        relationObject = matchIncludedToRelation(relationObject, included);

        deserializer.setField(object, relationshipNames.get(relationship), relationObject);
      } catch (JSONException e) {
        Logger.debug("JSON relationship does not contain data");
      }

      //map json array of data
      JSONArray relationDataArray = null;
      try {
        relationDataArray = relationJsonObject.getJSONArray("data");
        List<Resource> relationArray = Factory.newObjectFromJSONArray(relationDataArray, null);

        relationArray = matchIncludedToRelation(relationArray, included);

        deserializer.setField(object, relationshipNames.get(relationship), relationArray);
      } catch (JSONException e) {
        Logger.debug("JSON relationship does not contain data");
      }
    }

    return object;
  }


  /**
   * Will check if the relation is included. If true included object will be returned.
   *
   * @param object Relation resources.
   * @param included List of included resources.
   * @return Relation of included resource.
   */
  public Resource matchIncludedToRelation(Resource object, List<Resource> included) {
    if (included == null) {
      return object;
    }

    for (Resource resource : included) {
      if (object.getId().equals(resource.getId()) && object.getClass().equals(resource.getClass())) {
        return resource;
      }
    }
    return object;
  }

  /**
   * Loops through relations and calls {@link #matchIncludedToRelation(Resource, List)}.
   *
   * @param relationResources List of relation resources.
   * @param included List of included resources.
   * @return List of relations and/or included resources.
   */
  public List<Resource> matchIncludedToRelation(List<Resource> relationResources, List<Resource> included) {
    List<Resource> matchedResources = new ArrayList<>();
    for (Resource resource : relationResources) {
      matchedResources.add(matchIncludedToRelation(resource, included));
    }
    return matchedResources;
  }

  public List<Error> mapErrors(JSONArray errorArray) {
    List<Error> errors = new ArrayList<>();

    for (int i = 0; errorArray.length() > i; i++) {
      JSONObject errorJsonObject;
      try {
        errorJsonObject = errorArray.getJSONObject(i);
      } catch (JSONException e) {
        Logger.debug("No index " + i + " in error json array");
        continue;
      }
      Error error = new Error();

      try {
        error.setId(errorJsonObject.getString("id"));
      } catch (JSONException e) {
        Logger.debug("JSON object does not contain id");
      }

      try {
        error.setStatus(errorJsonObject.getString("status"));
      } catch (JSONException e) {
        Logger.debug("JSON object does not contain status");
      }

      try {
        error.setCode(errorJsonObject.getString("code"));
      } catch (JSONException e) {
        Logger.debug("JSON object does not contain code");
      }

      try {
        error.setTitle(errorJsonObject.getString("title"));
      } catch (JSONException e) {
        Logger.debug("JSON object does not contain title");
      }

      try {
        error.setDetail(errorJsonObject.getString("detail"));
      } catch (JSONException e) {
        Logger.debug("JSON object does not contain detail");
      }

      JSONObject sourceJsonObject = null;
      try {
        sourceJsonObject = errorJsonObject.getJSONObject("source");
      }
      catch (JSONException e) {
        Logger.debug("JSON object does not contain source");
      }

      if (sourceJsonObject != null) {
        Source source = new Source();
        try {
          source.setParameter(sourceJsonObject.getString("parameter"));
        } catch (JSONException e) {
          Logger.debug("JSON object does not contain parameter");
        }
        try {
          source.setPointer(sourceJsonObject.getString("pointer"));
        } catch (JSONException e) {
          Logger.debug("JSON object does not contain pointer");
        }
        error.setSource(source);
      }

      try {
        JSONObject linksJsonObject = errorJsonObject.getJSONObject("links");
        ErrorLinks links = new ErrorLinks();
        links.setAbout(linksJsonObject.getString("about"));
        error.setLinks(links);
      }
      catch (JSONException e) {
        Logger.debug("JSON object does not contain links or about");
      }

      try {
        error.setMeta(attributeMapper.createMapFromJSONObject(errorJsonObject.getJSONObject("meta")));
      } catch (JSONException e) {
        Logger.debug("JSON object does not contain JSONObject meta");
      }

      errors.add(error);
    }

    return errors;
  }

  public HashMap<String, ArrayList> createDataFromJsonResources(List<Resource> resources, boolean includeAttributes) {
    String resourceName = null;
    try {
      resourceName = nameForResourceClass(resources.get(0).getClass());
    } catch (Exception e) {
      Logger.debug(e.getMessage());
      return null;
    }

    HashMap<String, ArrayList> data = new HashMap<>();

    ArrayList<HashMap<String, Object>> dataArray = new ArrayList<>();

    for (Resource resource : resources) {
      HashMap<String, Object> attributes = serializer.getFieldsAsDictionary(resource);

      HashMap<String, Object> resourceRepresentation = new HashMap<>();
      resourceRepresentation.put("type", resourceName);
      resourceRepresentation.put("id", resource.getId());
      if (includeAttributes) {
        resourceRepresentation.put("attributes", attributes);
      }

      dataArray.add(resourceRepresentation);
    }

    data.put("data", dataArray);

    return data;
  }

  public HashMap<String, Object> createDataFromJsonResource(Resource resource,
                                                            boolean includeAttributes) {
    String resourceName = null;
    try {
      resourceName = nameForResourceClass(resource.getClass());
    } catch (Exception e) {
      Logger.debug(e.getMessage());
      return null;
    }

    HashMap<String, Object> resourceRepresentation = new HashMap<>();
    resourceRepresentation.put("type", resourceName);
    resourceRepresentation.put("id", resource.getId());
    if (includeAttributes) {
      HashMap<String, Object> attributes = serializer.getFieldsAsDictionary(resource);
      resourceRepresentation.put("attributes", attributes);
    }

    HashMap<String, Object> relationships = createRelationshipsFromResource(resource);
    if (relationships != null) {
      resourceRepresentation.put("relationships", relationships);
    }

    HashMap<String, Object> data = new HashMap<>();
    data.put("data", resourceRepresentation);

    return data;
  }

  public HashMap<String, Object> createRelationshipsFromResource(Resource resource) {
    HashMap<String, Object> relations = serializer.getRelationships(resource);
    HashMap<String, Object> relationships = new HashMap<>();

    for (String relationshipName : relations.keySet()) {
      Object relationObject = relations.get(relationshipName);
      if (relationObject instanceof Resource) {
        HashMap<String, Object> data = createDataFromJsonResource((Resource) relationObject, false);
        if (data != null) {
          relationships.put(relationshipName, data);
        }
      }

      if (relationObject instanceof ArrayList) {
        HashMap data = createDataFromJsonResources((List) relationObject, false);
        if (data != null) {
          relationships.put(relationshipName, data);
        }
      }
    }

    if (relationships.isEmpty()) {
      relationships = null;
    }

    return relationships;
  }

  // helper

  /**
   * Get the annotated relationship names.
   *
   * @param clazz Class for annotation.
   * @return List of relationship names.
   */
  private HashMap<String, String> getRelationshipNames(Class clazz) {
    HashMap<String, String> relationNames = new HashMap<>();
    for (Field field : clazz.getDeclaredFields()) {
      String fieldName = field.getName();
      for (Annotation annotation : field.getDeclaredAnnotations()) {
        if (annotation.annotationType() == SerializedName.class) {
          SerializedName serializeName = (SerializedName)annotation;
          fieldName = serializeName.value();
        }
        if (annotation.annotationType() == Relationship.class) {
          Relationship relationshipAnnotation = (Relationship)annotation;
          relationNames.put(relationshipAnnotation.value(), fieldName);
        }
      }
    }

    return relationNames;
  }

  private String nameForResourceClass(Class clazz) throws Exception {
     for (String key : Deserializer.getRegisteredClasses().keySet()) {
      if (Deserializer.getRegisteredClasses().get(key) == clazz) {
        return key;
      }
    }

    throw new Exception("Class " + clazz.getSimpleName() + " not registered.");
  }

  // getter

  public Deserializer getDeserializer() {
    return deserializer;
  }

  public AttributeMapper getAttributeMapper() {
    return attributeMapper;
  }

  public Serializer getSerializer() {
    return serializer;
  }
}
