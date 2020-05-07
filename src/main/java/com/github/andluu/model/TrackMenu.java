package com.github.andluu.model;


import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "chatId")
@ToString

@Entity
public class TrackMenu implements Serializable {
    @Id
    private long chatId;
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Track> foundTracks;

}
