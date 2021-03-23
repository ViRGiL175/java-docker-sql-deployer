package ru.virgil7;

import java.io.Serializable;
import java.util.Set;

class ConnectionData implements Serializable {

    public Set<String> names;
    public Set<Integer> ports;

    public ConnectionData(Set<String> names, Set<Integer> ports) {
        this.names = names;
        this.ports = ports;
    }
}
