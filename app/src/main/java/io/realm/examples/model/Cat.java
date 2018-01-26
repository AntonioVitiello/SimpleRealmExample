package io.realm.examples.model;

/**
 * Created by antlap on 28/10/2017.
 */

import io.realm.RealmObject;

public class Cat extends RealmObject {
    public String name;

    @Override
    public String toString() {
        return name;
    }
}
