export interface TaxiLocation {
  latitude: number;
  longitude: number;
}

export interface TaxiStatus {
  available: boolean;
  occupied: boolean;
}

export interface Taxi {
  id: string;
  driverName: string;
  licensePlate: string;
  location: TaxiLocation;
  status: TaxiStatus;
  lastUpdate: Date;
}

// In-memory storage para los taxis
class TaxiModel {
  private taxis: Map<string, Taxi> = new Map();

  addOrUpdateTaxi(taxi: Taxi): void {
    this.taxis.set(taxi.id, {
      ...taxi,
      lastUpdate: new Date(),
    });
  }

  getTaxi(id: string): Taxi | undefined {
    return this.taxis.get(id);
  }

  getAllTaxis(): Taxi[] {
    return Array.from(this.taxis.values());
  }

  removeTaxi(id: string): boolean {
    return this.taxis.delete(id);
  }
}

export default new TaxiModel();
