package com.themis.engine.domain;

/**
 * Tracks the state of a character's action economy during a single turn.
 * In Pathfinder 1e, a character gets:
 * - 1 Standard Action
 * - 1 Move Action
 * - 1 Swift Action
 * - Unlimited Free Actions (within reason)
 * 
 * Rules:
 * - Consuming a standard action prevents taking a full-round action.
 * - Consuming a move action prevents taking a full-round action.
 * - Consuming a full-round action consumes both standard and move actions.
 * - A standard action can be traded (downgraded) for an extra move action.
 */
public class TurnState implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private boolean standardUsed;
    private boolean moveUsed;
    private boolean swiftUsed;

    public TurnState() {
        reset();
    }

    /**
     * Resets the turn state (e.g. at the start of a character's new turn).
     */
    public final void reset() {
        this.standardUsed = false;
        this.moveUsed = false;
        this.swiftUsed = false;
    }

    /**
     * Restores the turn state directly from persisted values.
     */
    public void restore(boolean standardUsed, boolean moveUsed, boolean swiftUsed) {
        this.standardUsed = standardUsed;
        this.moveUsed = moveUsed;
        this.swiftUsed = swiftUsed;
    }

    public boolean isStandardUsed() {
        return standardUsed;
    }

    public boolean isMoveUsed() {
        return moveUsed;
    }

    public boolean isSwiftUsed() {
        return swiftUsed;
    }

    /**
     * Checks if the specified ActionType is currently available to be consumed.
     */
    public boolean canConsume(ActionType actionType) {
        if (actionType == null) {
            throw new IllegalArgumentException("ActionType cannot be null");
        }
        return switch (actionType) {
            case STANDARD -> !standardUsed;
            case MOVE -> !moveUsed || !standardUsed; // Can use Standard to move
            case SWIFT -> !swiftUsed;
            case FREE -> true;
            case FULL_ROUND -> !standardUsed && !moveUsed;
        };
    }

    /**
     * Consumes the specified ActionType.
     * Throws IllegalStateException if the action cannot be consumed.
     */
    public void consume(ActionType actionType) {
        if (!canConsume(actionType)) {
            throw new IllegalStateException("Cannot consume action type: " + actionType + " in current turn state");
        }

        switch (actionType) {
            case STANDARD -> standardUsed = true;
            case MOVE -> {
                if (!moveUsed) {
                    moveUsed = true;
                } else {
                    // Downgrade standard action to move action
                    standardUsed = true;
                }
            }
            case SWIFT -> swiftUsed = true;
            case FREE -> {
                // Free actions are free and don't change state
            }
            case FULL_ROUND -> {
                standardUsed = true;
                moveUsed = true;
            }
        }
    }
}
