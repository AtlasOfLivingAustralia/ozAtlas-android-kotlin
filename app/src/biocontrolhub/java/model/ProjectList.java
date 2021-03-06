package model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by sad038 on 25/5/17.
 */

public class ProjectList {
    @Expose
    @SerializedName("total")
    public Integer total;

    @Expose
    @SerializedName("projects")
    public List<Projects> projects;

    //@Expose
    //public Facets[] facets;
}
