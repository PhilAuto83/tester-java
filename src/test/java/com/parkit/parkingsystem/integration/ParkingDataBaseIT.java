package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
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
    public void testParkingACar() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability
        assertNotNull(ticketDAO.getTicket("ABCDEF"));
        assertEquals(1, ticketDAO.getTicket("ABCDEF").getParkingSpot().getId());
        assertEquals(0.0, ticketDAO.getTicket("ABCDEF").getPrice());
        assertNull(ticketDAO.getTicket("ABCDEF").getOutTime());
    }


    @Test
    public void testParkingABike() {
        when(inputReaderUtil.readSelection()).thenReturn(2);
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability
        assertNotNull(ticketDAO.getTicket("ABCDEF"));
        assertEquals(4, ticketDAO.getTicket("ABCDEF").getParkingSpot().getId());
        assertFalse(ticketDAO.getTicket("ABCDEF").getParkingSpot().isAvailable());
        assertEquals(0.0, ticketDAO.getTicket("ABCDEF").getPrice());
        assertNull(ticketDAO.getTicket("ABCDEF").getOutTime());
    }

    @Test
    public void testParkingLotExit(){
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();
        //TODO: check that the fare generated and out time are populated correctly in the database
        assertNotNull(ticketDAO.getTicket("ABCDEF"));
        assertEquals(1, ticketDAO.getTicket("ABCDEF").getParkingSpot().getId());
        assertInstanceOf(Double.class,ticketDAO.getTicket("ABCDEF").getPrice());
        assertInstanceOf(Date.class,ticketDAO.getTicket("ABCDEF").getOutTime());
        assertNotNull(ticketDAO.getTicket("ABCDEF").getOutTime());
    }

}
