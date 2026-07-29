package de.kugi.dev.battleoftheuniverse.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/** Singleton row (fixed {@code id = 1}) holding global, admin-editable app settings; seeded by its Liquibase changeset. */
@Entity
@Table(name = "app_settings")
@Getter
@Setter
@NoArgsConstructor
public class AppSettings implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    private boolean registrationEnabled = true;
}
