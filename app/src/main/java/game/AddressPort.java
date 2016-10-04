package game;

import java.net.InetAddress;
import java.util.Arrays;

/**
 * Created by Neil on 8/29/2016.
 */
public class AddressPort {
    InetAddress address;
    int port;

    public AddressPort(InetAddress address, int port){
        this.address = address;
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AddressPort that = (AddressPort) o;

        return port == that.port && Arrays.equals(address.getAddress(), that.address.getAddress());
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(address.getAddress());
        result = 31 * result + port;
        return result;
    }
}
