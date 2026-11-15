package com.example.cartservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "carts")
public class Cart {
    public enum Status {
        OPEN,
        CHECKED_OUT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    public Cart() {
    }

    public Long getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
