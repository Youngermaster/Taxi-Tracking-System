import express from "express";
import http from "http";
import cors from "cors";
import config from "./config/config";
import SocketController from "./controllers/SocketController";

// Inicializar express
const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// Ruta básica para verificar que el servidor está funcionando
app.get("/", (req, res) => {
  res.json({
    message: "Taxi Tracking System API",
    status: "running",
    timestamp: new Date().toISOString(),
  });
});

// Inicializar servidor HTTP
const server = http.createServer(app);

// Inicializar controlador de websockets
new SocketController(server);

// Iniciar servidor
server.listen(config.port, () => {
  console.log(
    `Servidor corriendo en modo ${config.nodeEnv} en el puerto ${config.port}`
  );
});
