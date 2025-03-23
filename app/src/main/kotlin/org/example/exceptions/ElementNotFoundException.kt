package org.example.exceptions

class ElementNotFoundException(
    override val message: String = "Element not found",
) : RuntimeException()
