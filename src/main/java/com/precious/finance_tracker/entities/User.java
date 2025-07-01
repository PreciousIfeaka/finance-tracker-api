package com.precious.finance_tracker.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.precious.finance_tracker.enums.Currency;
import com.precious.finance_tracker.enums.Roles;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;


@Entity(name = "_user")
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User extends AbstractBaseEntity implements UserDetails {
    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private Boolean isVerified = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private Roles role = Roles.USER;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column()
    private String otp;

    @Column()
    private LocalDateTime otpExpiredAt;

    private String avatarUrl;

    private Boolean limitExceeded;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Income> incomes;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Expense> expenses;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Budget> budget;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
