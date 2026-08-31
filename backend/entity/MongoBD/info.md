estan las colecciones 
hay index id basicos
faltan los indexes nuevos

# INDEX contexto_dispositivo
deviceFingerprint 1 (asc)
clienteRef 1 (asc)
timestamp -1 (des)

# INDEX evento_transacción
codigoTranscaction 1 (asc)
timestamp -1 (des)
tipoEvento 1 (asc)

# INDEX log_integracion
correlationID 1 (asc)
endpoint 1 (asc)
timestamp -1 (des)
status 1 (asc)

# INDEX snapshot_decision
codigoTransaction 1 (asc)
timestamp -1 (des)
rawScore 1 (asc)

---