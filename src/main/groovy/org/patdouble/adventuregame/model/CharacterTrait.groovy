package org.patdouble.adventuregame.model

trait CharacterTrait {
    @Delegate
    Persona persona
    String nickName
    String fullName
    Room room
}
