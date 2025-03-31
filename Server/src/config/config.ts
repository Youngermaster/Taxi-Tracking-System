import dotenv from "dotenv";

// Cargar variables de entorno
dotenv.config();

export default {
  port: process.env.PORT || 3000,
  nodeEnv: process.env.NODE_ENV || "development",
};
