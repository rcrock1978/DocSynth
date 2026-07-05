package com.docsynth.application.audit;

/**
 * Port every *UseCase class receives (constructor injection) to emit
 * at least one audit event per transactional boundary.
 *
 * ArchUnit rule (T025c) enforces that any class whose name ends in
 * `UseCase` declares a constructor parameter of this type.
 */
public interface AuditEmitter {
    void emit(AuditEventEnvelope envelope);
}
