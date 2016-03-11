package at.rags.morpheusexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import at.rags.morpheus.JSONAPIObject;
import at.rags.morpheus.Morpheus;
import at.rags.morpheus.Deserializer;
import at.rags.morpheusexample.JsonApiResources.Article;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }
}
