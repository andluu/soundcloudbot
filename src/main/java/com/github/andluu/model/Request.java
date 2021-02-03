package com.github.andluu.model;


import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString

@Entity
public class Request implements Serializable {
    @Id
    @GeneratedValue
    private long id;
    @NotNull
    private long chatId;
    @NotNull
    @Lob
    private String message;
    @Lob
    private String err;
    @NotNull
    private LocalDate at;
}
