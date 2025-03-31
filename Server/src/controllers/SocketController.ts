import { Server, Socket } from "socket.io";
import http from "http";
import TaxiModel, { Taxi } from "../models/TaxiModel";

// Tipos de eventos
enum SocketEvents {
  CONNECT = "connection",
  DISCONNECT = "disconnect",
  TAXI_LOCATION_UPDATE = "taxi:location:update",
  TAXI_STATUS_UPDATE = "taxi:status:update",
  TAXI_CONNECTED = "taxi:connected",
  TAXI_DISCONNECTED = "taxi:disconnected",
  ADMIN_GET_ALL_TAXIS = "admin:getAllTaxis",
  ALL_TAXIS_UPDATE = "all:taxis:update",
}

export default class SocketController {
  private io: Server;
  private adminSockets: Set<string> = new Set();
  private taxiSockets: Map<string, string> = new Map(); // taxiId -> socketId

  constructor(server: http.Server) {
    this.io = new Server(server, {
      cors: {
        origin: "*", // En producción, especificar orígenes permitidos
        methods: ["GET", "POST"],
      },
    });
    this.setupEventHandlers();
  }

  private setupEventHandlers(): void {
    this.io.on(SocketEvents.CONNECT, (socket: Socket) => {
      console.log(`Cliente conectado: ${socket.id}`);

      // Conexión de un taxi
      socket.on(SocketEvents.TAXI_CONNECTED, (taxiData: Taxi) => {
        console.log(`Taxi conectado: ${taxiData.id} - ${taxiData.driverName}`);
        this.taxiSockets.set(taxiData.id, socket.id);
        TaxiModel.addOrUpdateTaxi(taxiData);
        this.emitToAdmins(
          SocketEvents.ALL_TAXIS_UPDATE,
          TaxiModel.getAllTaxis()
        );
      });

      // Solicitud de todos los taxis por parte de admin
      socket.on(SocketEvents.ADMIN_GET_ALL_TAXIS, () => {
        console.log(`Admin solicitó lista de taxis: ${socket.id}`);
        this.adminSockets.add(socket.id);
        socket.emit(SocketEvents.ALL_TAXIS_UPDATE, TaxiModel.getAllTaxis());
      });

      // Actualización de ubicación de taxi
      socket.on(
        SocketEvents.TAXI_LOCATION_UPDATE,
        (data: {
          taxiId: string;
          location: { latitude: number; longitude: number };
        }) => {
          const taxi = TaxiModel.getTaxi(data.taxiId);
          if (taxi) {
            taxi.location = data.location;
            taxi.lastUpdate = new Date();
            TaxiModel.addOrUpdateTaxi(taxi);
            this.emitToAdmins(
              SocketEvents.ALL_TAXIS_UPDATE,
              TaxiModel.getAllTaxis()
            );
          }
        }
      );

      // Actualización de estado de taxi
      socket.on(
        SocketEvents.TAXI_STATUS_UPDATE,
        (data: {
          taxiId: string;
          status: { available: boolean; occupied: boolean };
        }) => {
          const taxi = TaxiModel.getTaxi(data.taxiId);
          if (taxi) {
            taxi.status = data.status;
            taxi.lastUpdate = new Date();
            TaxiModel.addOrUpdateTaxi(taxi);
            this.emitToAdmins(
              SocketEvents.ALL_TAXIS_UPDATE,
              TaxiModel.getAllTaxis()
            );
          }
        }
      );

      // Manejo de desconexión
      socket.on(SocketEvents.DISCONNECT, () => {
        console.log(`Cliente desconectado: ${socket.id}`);

        // Eliminar de administradores si es uno
        this.adminSockets.delete(socket.id);

        // Buscar si es un taxi y eliminarlo
        for (const [taxiId, socketId] of this.taxiSockets.entries()) {
          if (socketId === socket.id) {
            console.log(`Taxi desconectado: ${taxiId}`);
            this.taxiSockets.delete(taxiId);
            TaxiModel.removeTaxi(taxiId);
            this.emitToAdmins(SocketEvents.TAXI_DISCONNECTED, { taxiId });
            this.emitToAdmins(
              SocketEvents.ALL_TAXIS_UPDATE,
              TaxiModel.getAllTaxis()
            );
            break;
          }
        }
      });
    });
  }

  private emitToAdmins(event: string, data: any): void {
    this.adminSockets.forEach((socketId) => {
      this.io.to(socketId).emit(event, data);
    });
  }
}
