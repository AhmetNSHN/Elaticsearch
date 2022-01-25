package com.works.documents;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.persistence.Id;
import java.util.UUID;

@Document(indexName = "customer")
@Data
public class ElasticCustomer {

    @Id
    private String id = UUID.randomUUID().toString();

    @Field(type= FieldType.Text)
    private Integer cid;

    @Field(type= FieldType.Text)
    private String name;

    @Field(type= FieldType.Text)
    private String email;


}
