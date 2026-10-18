package com.streamworx.userservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "root")
public class PostXmlResponse {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "row")
    private List<PostDto> rows = new ArrayList<>();

    public PostXmlResponse() {
    }

    public PostXmlResponse(List<PostDto> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }

    public List<PostDto> getRows() {
        return rows;
    }

    public void setRows(List<PostDto> rows) {
        this.rows = rows;
    }
}
