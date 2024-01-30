package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;


public class FareCalculatorService {

    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }else if(ticket.getOutTime().getTime()-ticket.getInTime().getTime()<(30*60*1000)){
            ticket.setPrice(0);
        }else{
            long inHour = ticket.getInTime().getTime();
            long outHour = ticket.getOutTime().getTime();

            //TODO: Some tests are failing here. Need to check if this logic is correct
            double duration = (double)(outHour-inHour)/(1000*60*60);
            double roundHoursTo2Decimals = Math.round(duration*100.00)/100.00;

            switch (ticket.getParkingSpot().getParkingType()){
                case CAR: {
                    ticket.setPrice(roundHoursTo2Decimals * Fare.CAR_RATE_PER_HOUR);
                    break;
                }
                case BIKE: {
                    ticket.setPrice(roundHoursTo2Decimals * Fare.BIKE_RATE_PER_HOUR);
                    break;
                }
                default: throw new IllegalArgumentException("Unknown Parking Type");
            }
        }


    }
}