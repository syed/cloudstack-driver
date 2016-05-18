package org.apache.cloudstack.storage.datastore.utils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class DateraMapKeysAdapter implements JsonDeserializer<Map<String, Object>>
{
  @Override
  public Map<String, Object> deserialize(JsonElement json, Type unused, JsonDeserializationContext context)
      throws JsonParseException
  {

    if (!json.isJsonObject()) throw new JsonParseException("Datera Hashmap : Invalid json");

    Map<String, Object> result = new HashMap<String, Object> ();
    JsonObject jsonObject = json.getAsJsonObject();
    for (Entry<String, JsonElement> entry : jsonObject.entrySet())
    {
      String key = entry.getKey();
      JsonElement element = entry.getValue();
      if (element.isJsonPrimitive())
      {
        result.put(key, element.getAsString());
      }
      else if (element.isJsonObject())
      {
        result.put(key, element);
      }
      // if not handling nulls and arrays
      else
      {
        throw new JsonParseException("Datera Hashmap : Could not parse the object in the list");
      }
    }
    return result;
  }
}
