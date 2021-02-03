package com.github.andluu.model;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
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
    @Lob
    private String performer;
    @Lob
    private String title;
    @Lob
    private String url;
    @Transient
    @ToString.Exclude
    private File file;
}
