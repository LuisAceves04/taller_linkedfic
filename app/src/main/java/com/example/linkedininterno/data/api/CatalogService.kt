package com.example.linkedininterno.data.api

import com.example.linkedininterno.data.model.IndustryDto
import com.example.linkedininterno.data.model.MajorDto
import retrofit2.Response
import retrofit2.http.GET

interface CatalogService {

    @GET("catalogs/majors")
    suspend fun getMajors(): Response<List<MajorDto>>

    @GET("catalogs/industries")
    suspend fun getIndustries(): Response<List<IndustryDto>>
}
