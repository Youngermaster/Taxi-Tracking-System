# Taxi Tracking System - Servidor WebSocket

Este servidor proporciona una interfaz bidireccional con WebSockets para la comunicación entre taxis y administradores en tiempo real.

## Tecnologías utilizadas

- Node.js
- Express
- Socket.io
- TypeScript

## Requisitos

- Node.js 14.x o superior
- npm 6.x o superior

## Instalación

```bash
# Instalar dependencias
npm install

# Compilar TypeScript
npm run build
```

## Ejecución

```bash
# Modo desarrollo (con recarga automática)
npm run dev

# Modo producción
npm run build
npm start
```

## Variables de entorno

Crear un archivo `.env` en la raíz del proyecto con las siguientes variables:

```
PORT=3000
NODE_ENV=development
```

## Eventos de WebSocket

### Eventos de Taxi

- `taxi:connected`: Un taxi se conecta al sistema
- `taxi:location:update`: Actualiza la ubicación de un taxi
- `taxi:status:update`: Actualiza el estado de un taxi (disponible/ocupado)

### Eventos de Admin

- `admin:getAllTaxis`: Solicita la lista de todos los taxis
- `all:taxis:update`: Recibe actualizaciones de todos los taxis
- `taxi:disconnected`: Notifica cuando un taxi se desconecta

## Estructura de datos

### Taxi

```typescript
{
  id: string;
  driverName: string;
  licensePlate: string;
  location: {
    latitude: number;
    longitude: number;
  }
  status: {
    available: boolean;
    occupied: boolean;
  }
  lastUpdate: Date;
}
```
