package at.rags.morpheus.Resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import at.rags.morpheus.Resource;
import at.rags.morpheus.annotations.JsonApiType;

@JsonApiType("ChatRoom")
public class ChatRoom extends Resource implements Serializable {

    @SerializedName("pin")
    private String pin;

    public String getPin() {
        return pin;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(ChatRoom.class, new ResourceSerializer<ChatRoom>())
            .create();
        return gson.toJson(this);
    }
}
