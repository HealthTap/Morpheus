package at.rags.morpheus;

import android.test.InstrumentationTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.InputStream;

import at.rags.morpheus.Resources.Article;
import at.rags.morpheus.Resources.Author;
import at.rags.morpheus.Resources.Comment;

@RunWith(JUnit4.class)
public class MorpheusMappingTests extends InstrumentationTestCase {

  @Test
  public void testInit() throws Exception {
    Morpheus morpheus = new Morpheus();
    assertNotNull(morpheus);
  }

  @Test
  public void testDataArray() throws Exception {
    Morpheus morpheus = new Morpheus();
    morpheus.registerResourceClass("articles", Article.class);
    morpheus.registerResourceClass("people", Author.class);
    morpheus.registerResourceClass("comments", Comment.class);

    JSONAPIObject jsonapiObject =
        morpheus.jsonToObject(loadJSONFromAsset(R.raw.articles));

    assertTrue(jsonapiObject.getResources().size() == 1);
    assertTrue(jsonapiObject.getResources().get(0).getClass() == Article.class);
    Article article = (Article)jsonapiObject.getResources().get(0);
    assertTrue(article.getId().equals("1"));
    assertTrue(article.getTitle().equals("JSON API paints my bikeshed!"));
  }

  @Test
  public void testDataObject() throws Exception {
    Morpheus morpheus = new Morpheus();
    morpheus.registerResourceClass("articles", Article.class);
    morpheus.registerResourceClass("people", Author.class);
    morpheus.registerResourceClass("comments", Comment.class);

    JSONAPIObject jsonapiObject =
        morpheus.jsonToObject(loadJSONFromAsset(R.raw.article));

    assertNotNull(jsonapiObject.getResource());
    assertTrue(jsonapiObject.getResource().getClass() == Article.class);
    Article article = (Article)jsonapiObject.getResource();
    assertTrue(article.getId().equals("1"));
    assertTrue(article.getTitle().equals("JSON API paints my bikeshed!"));
  }


  @Test
  public void testRelationship() throws Exception {
    Morpheus morpheus = new Morpheus();
    morpheus.registerResourceClass("articles", Article.class);
    morpheus.registerResourceClass("people", Author.class);
    morpheus.registerResourceClass("comments", Comment.class);

    JSONAPIObject jsonapiObject =
        morpheus.jsonToObject(loadJSONFromAsset(R.raw.articles));

    assertTrue(jsonapiObject.getResources().size() == 1);
    assertTrue(jsonapiObject.getResources().get(0).getClass() == Article.class);
    Article article = (Article)jsonapiObject.getResources().get(0);
    assertNotNull(article.getAuthor());
    assertTrue(article.getComments().size() == 2);
  }

  @Test
  public void testIncluded() throws Exception {
    Morpheus morpheus = new Morpheus();
    morpheus.registerResourceClass("articles", Article.class);
    morpheus.registerResourceClass("people", Author.class);
    morpheus.registerResourceClass("comments", Comment.class);

    JSONAPIObject jsonapiObject =
        morpheus.jsonToObject(loadJSONFromAsset(R.raw.articles));

    assertTrue(jsonapiObject.getIncluded().size() == 3);
  }

  private String loadJSONFromAsset(int file) {
    String json = null;
    try {
      InputStream is = getInstrumentation().getContext().getResources().openRawResource(file);
      int size = is.available();
      byte[] buffer = new byte[size];
      is.read(buffer);
      is.close();
      json = new String(buffer, "UTF-8");
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return json;
  }
}