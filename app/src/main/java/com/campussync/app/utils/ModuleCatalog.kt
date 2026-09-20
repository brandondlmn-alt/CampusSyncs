package com.campussync.app.utils

/**
 * Single source of truth for Rosebank International academic data.
 */
object ModuleCatalog {

    data class Module(val code: String, val name: String)

    fun getModulesFor(year: Int, semester: Int): List<Module> {
        return when (year to semester) {
            (1 to 1) -> listOf(
                Module("BUIS5111", "Business Information Systems"),
                Module("PROG5121", "Programming 1A"),
                Module("IQTT5111", "Introduction to Quantitative Thinking and Techniques"),
                Module("PRLD5121", "Programming Logic and Design")
            )
            (1 to 2) -> listOf(
                Module("WEDE5020", "Web Development (Introduction)"),
                Module("PROG6112", "Programming 1B"),
                Module("OPSY5121", "Operating Systems 1A"),
                Module("ITPP5112", "IT Professional Practice")
            )
            (2 to 1) -> listOf(
                Module("SAND6221", "System Analysis and Design"),
                Module("ISEC6321", "Information Security"),
                Module("PROG6221", "Programming 2A"),
                Module("DATA6211", "Database (Introduction)")
            )
            (2 to 2) -> listOf(
                Module("PROG6212", "Programming 2B"),
                Module("DATA6222", "Database (Intermediate)"),
                Module("IPMA6212", "IT Project Management"),
                Module("HCIN6222", "Human Computer Interaction")
            )
            (3 to 1) -> listOf(
                Module("OPSC6311", "Open Source Coding (Introduction)"),
                Module("WEDE6021", "Web Development (Intermediate)"),
                Module("ADDB6311", "Advanced Databases"),
                Module("XISD5319", "Work Integrated Learning 3A")
            )
            (3 to 2) -> listOf(
                Module("OPSC6312", "Open Source Coding (Intermediate)"),
                Module("APPR6312", "Applied Programming"),
                Module("SQAT6322", "Software Quality and Testing"),
                Module("XISD6329", "Work Integrated Learning 3B")
            )
            else -> emptyList()
        }
    }

    val campusLocations = listOf(
        "Bloemfontein", "Braamfontein", "Cape Town", "Durban",
        "Nelson Mandela Bay", "Nelspruit", "Polokwane", "Pretoria"
    )

    val courses = listOf(
        "DISD — Diploma in Information Technology in Software Development"
    )

    val years = listOf("1", "2", "3")
    val semesters = listOf("1", "2")
}
