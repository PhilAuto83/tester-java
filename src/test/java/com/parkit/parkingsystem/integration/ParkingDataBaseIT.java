package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


import java.sql.*;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    @Spy
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    @DisplayName("Vérification de la création du ticket voiture en base de données")
    public void testParkingACar() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability
        assertNotNull(ticketDAO.getTicket("ABCDEF"));
        assertEquals(1, ticketDAO.getTicket("ABCDEF").getParkingSpot().getId());
        assertEquals(0.0, ticketDAO.getTicket("ABCDEF").getPrice());
        assertNull(ticketDAO.getTicket("ABCDEF").getOutTime());
        assertEquals(2, parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR));
    }

    @Test
    @DisplayName("Vérification de la création du ticket moto en base de données")
    public void testParkingABike() {
        when(inputReaderUtil.readSelection()).thenReturn(2);
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability
        assertNotNull(ticketDAO.getTicket("ABCDEF"));
        assertEquals(4, ticketDAO.getTicket("ABCDEF").getParkingSpot().getId());
        assertEquals(0.0, ticketDAO.getTicket("ABCDEF").getPrice());
        assertNull(ticketDAO.getTicket("ABCDEF").getOutTime());
        assertEquals(5, parkingSpotDAO.getNextAvailableSlot(ParkingType.BIKE));
    }


    @Test
    @DisplayName("Calcul du tarif pour une heure de stationnement moto sans remise")
    public void testParkingABikeForOneHour() {
        ParkingSpot parkingSpot = new ParkingSpot(4, ParkingType.BIKE,true);
        when(inputReaderUtil.readSelection()).thenReturn(2);
        Timestamp inTime = Timestamp.from(Instant.now().minusMillis(60*60*1000));
        try (Connection dbTestConnection = dataBaseTestConfig.getConnection();
             PreparedStatement ps = dbTestConnection.prepareStatement(DBConstants.SAVE_TICKET)){
            ps.setInt(1, parkingSpot.getId());
            ps.setString(2, "ABCDEF");
            ps.setDouble(3,0.0);
            ps.setTimestamp(4, inTime);
            ps.setTimestamp(5, null);
            assertEquals(1, ps.executeUpdate());
        }catch (SQLException  | ClassNotFoundException ex){
            ex.printStackTrace();
        }
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();
        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability
        assertNotNull(ticketDAO.getTicket("ABCDEF"));
        assertEquals(4, ticketDAO.getTicket("ABCDEF").getParkingSpot().getId());
        assertEquals(1.0, ticketDAO.getTicket("ABCDEF").getPrice());
    }

    @Test
    @DisplayName("Appliquer la remise de 5% sur une durée de stationnement d'une heure pour une moto")
    public void testParkingABikeForOneHourWithDiscount() {
        ParkingSpot parkingSpot = new ParkingSpot(4, ParkingType.BIKE,true);
        when(inputReaderUtil.readSelection()).thenReturn(2);
        Timestamp inTime = Timestamp.from(Instant.now().minusMillis(60*60*1000));
        try (Connection dbTestConnection = dataBaseTestConfig.getConnection();
             PreparedStatement ps = dbTestConnection.prepareStatement(DBConstants.SAVE_TICKET)){
            ps.setInt(1, parkingSpot.getId());
            ps.setString(2, "ABCDEF");
            ps.setDouble(3,0.0);
            ps.setTimestamp(4, inTime);
            ps.setTimestamp(5, null);
            assertEquals(1, ps.executeUpdate());
        }catch (SQLException  | ClassNotFoundException ex){
            ex.printStackTrace();
        }
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(3);
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();
        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability
        assertNotNull(ticketDAO.getTicket("ABCDEF"));
        assertEquals(4, ticketDAO.getTicket("ABCDEF").getParkingSpot().getId());
        assertEquals(0.95, ticketDAO.getTicket("ABCDEF").getPrice());
    }

    @Test
    @DisplayName("Appliquer le tarif gratuit pour une voiture avec vérification des insertions de date, prix en base de données")
    public void testCarParkingLotExitForFree(){

        testParkingACar();
        assertEquals(1,ticketDAO.getNbTicket("ABCDEF"));
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();
        //TODO: check that the fare generated and out time are populated correctly in the database
        assertNotNull(ticketDAO.getTicket("ABCDEF"));
        assertEquals(1, ticketDAO.getTicket("ABCDEF").getParkingSpot().getId());
        assertInstanceOf(Double.class,ticketDAO.getTicket("ABCDEF").getPrice());
        assertInstanceOf(Date.class,ticketDAO.getTicket("ABCDEF").getOutTime());
        assertNotNull(ticketDAO.getTicket("ABCDEF").getOutTime());
        assertEquals(0.0, ticketDAO.getTicket("ABCDEF").getPrice());

    }

    @Test
    @DisplayName("Calcul du tarif pour une heure de stationnement voiture sans remise")
    public void testCarParkingLotExitAfterOneHour() throws SQLException, ClassNotFoundException {

        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,true);
        Timestamp inTime = Timestamp.from(Instant.now().minusMillis(60*60*1000));
        try(Connection dbTestConnection = dataBaseTestConfig.getConnection();
            PreparedStatement ps = dbTestConnection.prepareStatement(DBConstants.SAVE_TICKET)) {

            ps.setInt(1, parkingSpot.getId());
            ps.setString(2, "ABCDEF");
            ps.setDouble(3,0.0);
            ps.setTimestamp(4, inTime);
            ps.setTimestamp(5, null);
            assertEquals(1, ps.executeUpdate());
        }catch (SQLException  | ClassNotFoundException ex){
            ex.printStackTrace();
        }
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();
        assertEquals(1.5, ticketDAO.getTicket("ABCDEF").getPrice());
    }

    @Test
    @DisplayName("Appliquer la remise de 5% sur une durée de stationnement de 2h pour une voiture")
    public void testCarParkingLotExitAfterOneHourWithDiscount() throws SQLException, ClassNotFoundException {

        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,true);
        Timestamp inTime = Timestamp.from(Instant.now().minusMillis(2*60*60*1000));
        try(Connection dbTestConnection = dataBaseTestConfig.getConnection();
            PreparedStatement ps = dbTestConnection.prepareStatement(DBConstants.SAVE_TICKET)) {
            ps.setInt(1, parkingSpot.getId());
            ps.setString(2, "ABCDEF");
            ps.setDouble(3,0.0);
            ps.setTimestamp(4, inTime);
            ps.setTimestamp(5, null);
            assertEquals(1, ps.executeUpdate());
        }catch (SQLException  | ClassNotFoundException ex){
            ex.printStackTrace();
        }
        when(ticketDAO.getNbTicket(anyString())).thenReturn(2);
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();
        assertEquals(Math.round(1.5*2*0.95*100.0)/100.0, ticketDAO.getTicket("ABCDEF").getPrice());
    }

}
