package nl.milean.missionrace_gm.missiondata;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by Michiel on 13-3-2016.
 */
public class Mission implements Serializable{
    private HashMap<String, Object> data;

    public Mission(MissionTypes type){
        this.data = new HashMap<String, Object>();
    }

    public Object getData(String key){
        return this.data.get(key);
    }

    public Boolean containsData(String key) { return this.data.containsKey(key); }

    public boolean addData(String key, Object value){
        if(this.data.containsKey(key)){
            return false;
        }
        else {
            this.data.put(key, value);
            return true;
        }
    }
}
