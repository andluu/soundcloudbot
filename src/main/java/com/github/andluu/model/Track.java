package com.github.andluu.model;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.File;
import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "url")
@ToString
@Builder

@Entity
public class Track implements Serializable {
    @Id
    private long id;
    private String performer;
    private String title;
    private String url;
    @Transient
    @ToString.Exclude
    private File file;
}
