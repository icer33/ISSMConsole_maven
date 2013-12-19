/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.pojos;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author Icer
 */
public class WorldPOJO implements Serializable{
    String sector;
    String coordinates;

    public WorldPOJO(String sector, String coordinates) {
        this.sector = sector;
        this.coordinates = coordinates;
    }

    public String getSector() {
        return sector;
    }

    public String getCoordinates() {
        return coordinates;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.sector);
        hash = 53 * hash + Objects.hashCode(this.coordinates);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final WorldPOJO other = (WorldPOJO) obj;
        if (!Objects.equals(this.sector, other.sector)) {
            return false;
        }
        if (!Objects.equals(this.coordinates, other.coordinates)) {
            return false;
        }
        return true;
    }
}
