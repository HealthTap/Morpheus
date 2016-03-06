package at.rags.morpheusexample.JsonApiResources;

import at.rags.morpheus.Annotations.SerializeName;

public class Article extends MyResource {

  @SerializeName(jsonName = "title")
  private String title;

  public String getTitle() {
    return title;
  }
}
