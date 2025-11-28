package com.example.eventconnect.remote.sync

import android.content.Context
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.PendingOperation
import com.example.eventconnect.remote.RetrofitProvider
import com.example.eventconnect.remote.service.ReviewService
import com.example.eventconnect.remote.service.UserReviewDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteReviewSync(private val context: Context) {
    private val service by lazy { RetrofitProvider.retrofit(context).create(ReviewService::class.java) }
    private val pendingDao = AppDatabase.getDatabase(context).pendingOperationDao()

    suspend fun submit(review: UserReviewDto) {
        withContext(Dispatchers.IO) {
            try {
                service.create(review)
            } catch (ex: Exception) {
                val payload = "{\"email\":\"${review.reviewedUserEmail}\",\"reviewerStore\":\"${review.reviewerStore}\",\"rating\":${review.rating},\"comment\":${if (review.comment==null)"null" else "\""+review.comment.replace("\"","\\\"")+"\""}}"
                pendingDao.insert(PendingOperation(type="USER_REVIEW_CREATE", payload=payload))
            }
        }
    }
}
