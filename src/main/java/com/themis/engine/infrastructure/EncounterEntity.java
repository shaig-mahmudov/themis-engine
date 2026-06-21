package com.themis.engine.infrastructure;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "encounters")
public class EncounterEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String status;

    @Column(name = "current_round", nullable = false)
    private int currentRound;

    @Column(name = "active_participant_index", nullable = false)
    private int activeParticipantIndex;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "encounter_id")
    @OrderColumn(name = "sort_order")
    private List<EncounterParticipantEntity> participants = new ArrayList<>();

    public EncounterEntity() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getActiveParticipantIndex() {
        return activeParticipantIndex;
    }

    public void setActiveParticipantIndex(int activeParticipantIndex) {
        this.activeParticipantIndex = activeParticipantIndex;
    }

    public List<EncounterParticipantEntity> getParticipants() {
        return participants;
    }

    public void setParticipants(List<EncounterParticipantEntity> participants) {
        this.participants = participants;
    }
}
