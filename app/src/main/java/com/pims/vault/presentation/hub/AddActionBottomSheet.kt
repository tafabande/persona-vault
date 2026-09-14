package com.pims.vault.presentation.hub

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.ui.util.rememberPimsHaptics

/**
 * Granular action types covering Persona's personal information universe.
 */
enum class AddActionType {
    // Identity
    PERSONAL_DETAILS,
    IDENTIFICATION_DOC,
    NAMES_HISTORY,
    EMERGENCY_IDENTITY,

    // Contact
    PHONE,
    EMAIL,
    ADDRESS,
    ONLINE_PRESENCE,
    COMMUNICATION_PREFS,

    // Education
    EDUCATION,
    QUALIFICATION,
    CERTIFICATION,
    ACADEMIC_ACHIEVEMENT,
    COURSES_TRAINING,
    SKILLS_LANGUAGES,

    // Career
    EXPERIENCE,
    PROJECTS,
    PORTFOLIO,
    AWARDS,
    VOLUNTEERING,
    PROFESSIONAL_MEMBERSHIPS,
    REFERENCES,

    // Health
    MEDICAL_HISTORY,
    ALLERGIES,
    MEDICATIONS,
    EMERGENCY_INFO,
    DOCTORS_PROVIDERS,
    HEALTH_DOCUMENTS,

    // Personal
    INTERESTS_HOBBIES,
    PREFERENCES,
    PERSONAL_NOTES,
    GOALS,
    IMPORTANT_DATES,
    FAVOURITE_THINGS,
    MEMBERSHIPS_AFFILIATIONS,

    // Top-Level / Direct
    DOCUMENT,
    CUSTOM_FIELD,
    PERSON,
    PASSWORD,
    PAYMENT_CARD,
    BANK_ACCOUNT,
    SOCIAL_PROFILE
}

private enum class AddSheetScreen {
    ROOT,
    CATEGORIES,
    DETAIL_IDENTITY,
    DETAIL_CONTACT,
    DETAIL_EDUCATION,
    DETAIL_CAREER,
    DETAIL_HEALTH,
    DETAIL_PERSONAL
}

/**
 * AddActionBottomSheet
 *
 * Implements progressive disclosure for Persona:
 * Level 1: "What would you like to add?" (Add person vs Add to my profile)
 * Level 2: 6 clean broad domain sections + Document + Custom information
 * Level 3: Tailored domain options mapping to the complete personal information universe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActionBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onSelectAction: (AddActionType) -> Unit
) {
    var currentScreen by remember { mutableStateOf(AddSheetScreen.ROOT) }
    val haptics = rememberPimsHaptics()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "AddActionScreenTransition"
            ) { screen ->
                when (screen) {
                    // =========================================================
                    // LEVEL 1: ROOT (Add person vs Add to my profile)
                    // =========================================================
                    AddSheetScreen.ROOT -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Add Information",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.3).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Select what you want to store",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = onDismissRequest) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // 1. Password
                            CategoryDomainCard(
                                icon = Icons.Default.Key,
                                title = "Password",
                                subtitle = "Website or app login credential",
                                isAccent = true,
                                onClick = {
                                    haptics.selection()
                                    onSelectAction(AddActionType.PASSWORD)
                                }
                            )

                            // 2. Payment Card
                            CategoryDomainCard(
                                icon = Icons.Default.CreditCard,
                                title = "Payment Card",
                                subtitle = "Credit / debit card with encrypted details",
                                onClick = {
                                    haptics.selection()
                                    onSelectAction(AddActionType.PAYMENT_CARD)
                                }
                            )

                            // 3. Bank Account
                            CategoryDomainCard(
                                icon = Icons.Default.Shield,
                                title = "Bank Account",
                                subtitle = "Account number, branch code & banking institution",
                                onClick = {
                                    haptics.selection()
                                    onSelectAction(AddActionType.BANK_ACCOUNT)
                                }
                            )

                            // 4. Medical Record
                            CategoryDomainCard(
                                icon = Icons.Default.MedicalServices,
                                title = "Medical Record",
                                subtitle = "Allergies, conditions, medications & emergency info",
                                onClick = {
                                    haptics.selection()
                                    onSelectAction(AddActionType.ALLERGIES)
                                }
                            )

                            // 5. Identity
                            CategoryDomainCard(
                                icon = Icons.Default.Badge,
                                title = "Identity",
                                subtitle = "Legal name, birth date, citizenship & identity data",
                                onClick = {
                                    haptics.selection()
                                    onSelectAction(AddActionType.PERSONAL_DETAILS)
                                }
                            )

                            // 6. Contact Details
                            CategoryDomainCard(
                                icon = Icons.Default.Phone,
                                title = "Contact",
                                subtitle = "Phone numbers, mobile lines & primary email",
                                onClick = {
                                    haptics.selection()
                                    onSelectAction(AddActionType.PHONE)
                                }
                            )

                            // 7. Residential Address
                            CategoryDomainCard(
                                icon = Icons.Default.Home,
                                title = "Address",
                                subtitle = "Residential address, postal code & location",
                                onClick = {
                                    haptics.selection()
                                    onSelectAction(AddActionType.ADDRESS)
                                }
                            )

                            // 8. Document
                            CategoryDomainCard(
                                icon = Icons.Default.Description,
                                title = "Document",
                                subtitle = "Upload national ID, passport, diploma or PDF",
                                onClick = {
                                    haptics.selection()
                                    onSelectAction(AddActionType.DOCUMENT)
                                }
                            )

                            // 9. Social Profile
                            CategoryDomainCard(
                                icon = Icons.Default.Public,
                                title = "Social Profile",
                                subtitle = "Instagram, WhatsApp, LinkedIn, GitHub & links",
                                onClick = {
                                    haptics.selection()
                                    onSelectAction(AddActionType.SOCIAL_PROFILE)
                                }
                            )

                            // 10. Professional & Education
                            CategoryDomainCard(
                                icon = Icons.Default.Work,
                                title = "Professional & Education",
                                subtitle = "Schools, degrees, career history & certifications",
                                onClick = {
                                    haptics.selection()
                                    onSelectAction(AddActionType.EDUCATION)
                                }
                            )

                            // 11. Person / Kin
                            CategoryDomainCard(
                                icon = Icons.Default.PersonAdd,
                                title = "Person / Kin",
                                subtitle = "Family member, emergency ICE contact, or connection",
                                onClick = {
                                    haptics.selection()
                                    onSelectAction(AddActionType.PERSON)
                                }
                            )

                            // 12. Custom Information
                            CategoryDomainCard(
                                icon = Icons.Default.Extension,
                                title = "Custom Field",
                                subtitle = "Flexible custom field with smart semantic normalization",
                                onClick = {
                                    haptics.selection()
                                    onSelectAction(AddActionType.CUSTOM_FIELD)
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // =========================================================
                    // LEVEL 2: 6 MAIN CATEGORIES + DOCUMENT + CUSTOM
                    // =========================================================
                    AddSheetScreen.CATEGORIES -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header with Back to Root
                            SheetHeaderWithBack(
                                title = "Add to my profile",
                                onBack = {
                                    haptics.light()
                                    currentScreen = AddSheetScreen.ROOT
                                },
                                onClose = onDismissRequest
                            )

                            // 1. Identity
                            CategoryDomainCard(
                                icon = Icons.Default.Badge,
                                title = "Identity",
                                subtitle = "Identity, names, identification & emergency ID",
                                onClick = {
                                    haptics.selection()
                                    currentScreen = AddSheetScreen.DETAIL_IDENTITY
                                }
                            )

                            // 2. Contact
                            CategoryDomainCard(
                                icon = Icons.Default.Phone,
                                title = "Contact",
                                subtitle = "Phone, email, addresses & online presence",
                                onClick = {
                                    haptics.selection()
                                    currentScreen = AddSheetScreen.DETAIL_CONTACT
                                }
                            )

                            // 3. Education
                            CategoryDomainCard(
                                icon = Icons.Default.School,
                                title = "Education",
                                subtitle = "Schools, qualifications, certifications & skills",
                                onClick = {
                                    haptics.selection()
                                    currentScreen = AddSheetScreen.DETAIL_EDUCATION
                                }
                            )

                            // 4. Career
                            CategoryDomainCard(
                                icon = Icons.Default.Work,
                                title = "Career",
                                subtitle = "Work experience, projects, portfolio & references",
                                onClick = {
                                    haptics.selection()
                                    currentScreen = AddSheetScreen.DETAIL_CAREER
                                }
                            )

                            // 5. Health
                            CategoryDomainCard(
                                icon = Icons.Default.Favorite,
                                title = "Health",
                                subtitle = "Medical conditions, allergies, medications & doctors",
                                onClick = {
                                    haptics.selection()
                                    currentScreen = AddSheetScreen.DETAIL_HEALTH
                                }
                            )

                            // 6. Personal
                            CategoryDomainCard(
                                icon = Icons.Default.Extension,
                                title = "Personal",
                                subtitle = "Interests, preferences, goals & important dates",
                                onClick = {
                                    haptics.selection()
                                    currentScreen = AddSheetScreen.DETAIL_PERSONAL
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // 7. Document (Direct action)
                            CategoryDomainCard(
                                icon = Icons.Default.Description,
                                title = "Document",
                                subtitle = "Store or upload an encrypted file about yourself",
                                onClick = {
                                    haptics.selection()
                                    onSelectAction(AddActionType.DOCUMENT)
                                }
                            )

                            // 8. Custom information (Direct action)
                            CategoryDomainCard(
                                icon = Icons.Default.AutoAwesome,
                                title = "Custom information",
                                subtitle = "Create your own type of field, identifier, or note",
                                isAccent = true,
                                onClick = {
                                    haptics.selection()
                                    onSelectAction(AddActionType.CUSTOM_FIELD)
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // =========================================================
                    // LEVEL 3: DETAIL - IDENTITY
                    // =========================================================
                    AddSheetScreen.DETAIL_IDENTITY -> {
                        DetailScreenLayout(
                            title = "Identity",
                            description = "Core identifiers and legal identity records",
                            onBack = { currentScreen = AddSheetScreen.CATEGORIES },
                            onClose = onDismissRequest
                        ) {
                            DetailItemRow(
                                icon = Icons.Default.Badge,
                                title = "Personal details",
                                subtitle = "Name, preferred name, DOB, nationality, occupation",
                                onClick = { onSelectAction(AddActionType.PERSONAL_DETAILS) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Description,
                                title = "Identification",
                                subtitle = "National ID, passport, driver's licence, birth cert",
                                onClick = { onSelectAction(AddActionType.IDENTIFICATION_DOC) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Person,
                                title = "Names & identity history",
                                subtitle = "Previous names, aliases, legal name changes",
                                onClick = { onSelectAction(AddActionType.NAMES_HISTORY) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Favorite,
                                title = "Emergency identity",
                                subtitle = "Blood type, emergency identifiers, critical information",
                                onClick = { onSelectAction(AddActionType.EMERGENCY_IDENTITY) }
                            )
                        }
                    }

                    // =========================================================
                    // LEVEL 3: DETAIL - CONTACT
                    // =========================================================
                    AddSheetScreen.DETAIL_CONTACT -> {
                        DetailScreenLayout(
                            title = "Contact",
                            description = "Ways to reach and connect with you",
                            onBack = { currentScreen = AddSheetScreen.CATEGORIES },
                            onClose = onDismissRequest
                        ) {
                            DetailItemRow(
                                icon = Icons.Default.Phone,
                                title = "Phone number",
                                subtitle = "Personal, work, secondary numbers",
                                onClick = { onSelectAction(AddActionType.PHONE) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Email,
                                title = "Email address",
                                subtitle = "Personal, work, academic, other",
                                onClick = { onSelectAction(AddActionType.EMAIL) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Home,
                                title = "Physical address",
                                subtitle = "Home, work, postal, previous addresses",
                                onClick = { onSelectAction(AddActionType.ADDRESS) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Public,
                                title = "Online presence",
                                subtitle = "Website, portfolio, social profiles, usernames",
                                onClick = { onSelectAction(AddActionType.ONLINE_PRESENCE) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Tune,
                                title = "Communication preferences",
                                subtitle = "Preferred contact method, availability notes",
                                onClick = { onSelectAction(AddActionType.COMMUNICATION_PREFS) }
                            )
                        }
                    }

                    // =========================================================
                    // LEVEL 3: DETAIL - EDUCATION
                    // =========================================================
                    AddSheetScreen.DETAIL_EDUCATION -> {
                        DetailScreenLayout(
                            title = "Education",
                            description = "Academic background, credentials and capabilities",
                            onBack = { currentScreen = AddSheetScreen.CATEGORIES },
                            onClose = onDismissRequest
                        ) {
                            DetailItemRow(
                                icon = Icons.Default.School,
                                title = "Education",
                                subtitle = "Schools, colleges, universities attended",
                                onClick = { onSelectAction(AddActionType.EDUCATION) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Description,
                                title = "Degree & qualification",
                                subtitle = "Degrees, diplomas, certificates earned",
                                onClick = { onSelectAction(AddActionType.QUALIFICATION) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Star,
                                title = "Professional certification",
                                subtitle = "Certifications, licences, registrations",
                                onClick = { onSelectAction(AddActionType.CERTIFICATION) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.EmojiEvents,
                                title = "Academic achievements",
                                subtitle = "Honours, awards, distinctions, scholarships",
                                onClick = { onSelectAction(AddActionType.ACADEMIC_ACHIEVEMENT) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Psychology,
                                title = "Courses & training",
                                subtitle = "Short courses, workshops, bootcamps, training",
                                onClick = { onSelectAction(AddActionType.COURSES_TRAINING) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Language,
                                title = "Skills & languages",
                                subtitle = "Technical skills, soft skills, spoken languages",
                                onClick = { onSelectAction(AddActionType.SKILLS_LANGUAGES) }
                            )
                        }
                    }

                    // =========================================================
                    // LEVEL 3: DETAIL - CAREER
                    // =========================================================
                    AddSheetScreen.DETAIL_CAREER -> {
                        DetailScreenLayout(
                            title = "Career",
                            description = "Professional life, projects and accomplishments",
                            onBack = { currentScreen = AddSheetScreen.CATEGORIES },
                            onClose = onDismissRequest
                        ) {
                            DetailItemRow(
                                icon = Icons.Default.Work,
                                title = "Work experience",
                                subtitle = "Jobs, roles, employers, career history",
                                onClick = { onSelectAction(AddActionType.EXPERIENCE) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Folder,
                                title = "Projects",
                                subtitle = "Personal, academic and professional projects",
                                onClick = { onSelectAction(AddActionType.PROJECTS) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Public,
                                title = "Portfolio",
                                subtitle = "Work you've created, links, descriptions",
                                onClick = { onSelectAction(AddActionType.PORTFOLIO) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.EmojiEvents,
                                title = "Awards & achievements",
                                subtitle = "Professional and personal achievements",
                                onClick = { onSelectAction(AddActionType.AWARDS) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.VolunteerActivism,
                                title = "Volunteering & activities",
                                subtitle = "Volunteer work, clubs, organisations",
                                onClick = { onSelectAction(AddActionType.VOLUNTEERING) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Groups,
                                title = "Professional memberships",
                                subtitle = "Associations, societies, registrations",
                                onClick = { onSelectAction(AddActionType.PROFESSIONAL_MEMBERSHIPS) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Person,
                                title = "References",
                                subtitle = "Professional or academic references",
                                onClick = { onSelectAction(AddActionType.REFERENCES) }
                            )
                        }
                    }

                    // =========================================================
                    // LEVEL 3: DETAIL - HEALTH
                    // =========================================================
                    AddSheetScreen.DETAIL_HEALTH -> {
                        DetailScreenLayout(
                            title = "Health",
                            description = "Medical conditions, medications and emergency records",
                            onBack = { currentScreen = AddSheetScreen.CATEGORIES },
                            onClose = onDismissRequest
                        ) {
                            DetailItemRow(
                                icon = Icons.Default.MedicalServices,
                                title = "Medical history",
                                subtitle = "Conditions, diagnoses, medical history",
                                onClick = { onSelectAction(AddActionType.MEDICAL_HISTORY) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Favorite,
                                title = "Allergies",
                                subtitle = "Food, medication, environmental allergies",
                                onClick = { onSelectAction(AddActionType.ALLERGIES) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Medication,
                                title = "Medications",
                                subtitle = "Current and previous medications, dosages",
                                onClick = { onSelectAction(AddActionType.MEDICATIONS) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.CheckCircle,
                                title = "Emergency information",
                                subtitle = "Critical medical information and emergency contacts",
                                onClick = { onSelectAction(AddActionType.EMERGENCY_INFO) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Person,
                                title = "Doctors & providers",
                                subtitle = "Doctors, clinics, hospitals, specialists",
                                onClick = { onSelectAction(AddActionType.DOCTORS_PROVIDERS) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Description,
                                title = "Health documents",
                                subtitle = "Prescriptions, reports, medical certificates",
                                onClick = { onSelectAction(AddActionType.HEALTH_DOCUMENTS) }
                            )
                        }
                    }

                    // =========================================================
                    // LEVEL 3: DETAIL - PERSONAL
                    // =========================================================
                    AddSheetScreen.DETAIL_PERSONAL -> {
                        DetailScreenLayout(
                            title = "Personal",
                            description = "Interests, lifestyle, reflections and milestones",
                            onBack = { currentScreen = AddSheetScreen.CATEGORIES },
                            onClose = onDismissRequest
                        ) {
                            DetailItemRow(
                                icon = Icons.Default.Extension,
                                title = "Interests & hobbies",
                                subtitle = "Activities, hobbies, creative pursuits",
                                onClick = { onSelectAction(AddActionType.INTERESTS_HOBBIES) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Tune,
                                title = "Preferences",
                                subtitle = "Travel, dining, lifestyle, personal preferences",
                                onClick = { onSelectAction(AddActionType.PREFERENCES) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Bookmark,
                                title = "Personal notes",
                                subtitle = "Private reflections, thoughts, personal notes",
                                onClick = { onSelectAction(AddActionType.PERSONAL_NOTES) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.FitnessCenter,
                                title = "Goals",
                                subtitle = "Personal, fitness, career or life milestones",
                                onClick = { onSelectAction(AddActionType.GOALS) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.CalendarToday,
                                title = "Important dates",
                                subtitle = "Birthdays, anniversaries, special occasions",
                                onClick = { onSelectAction(AddActionType.IMPORTANT_DATES) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Star,
                                title = "Favourite things",
                                subtitle = "Books, movies, music, places, memorable items",
                                onClick = { onSelectAction(AddActionType.FAVOURITE_THINGS) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.Groups,
                                title = "Memberships & affiliations",
                                subtitle = "Clubs, community groups, memberships",
                                onClick = { onSelectAction(AddActionType.MEMBERSHIPS_AFFILIATIONS) }
                            )
                            DetailItemRow(
                                icon = Icons.Default.AutoAwesome,
                                title = "Custom information",
                                subtitle = "Create your own field when not predefined",
                                onClick = { onSelectAction(AddActionType.CUSTOM_FIELD) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Standard reusable detail screen layout with back navigation and close button
 */
@Composable
private fun DetailScreenLayout(
    title: String,
    description: String,
    onBack: () -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    val haptics = rememberPimsHaptics()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SheetHeaderWithBack(
            title = title,
            onBack = {
                haptics.light()
                onBack()
            },
            onClose = onClose
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        content()
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun SheetHeaderWithBack(
    title: String,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Level 1 Choice Card
 */
@Composable
private fun PrimaryChoiceCard(
    icon: ImageVector,
    title: String,
    description: String,
    showChevron: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showChevron) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Level 2 Domain Category Card
 */
@Composable
private fun CategoryDomainCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isAccent: Boolean = false,
    onClick: () -> Unit
) {
    val haptics = rememberPimsHaptics()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isAccent) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress {
                haptics.selection()
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isAccent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isAccent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Level 3 Detailed Option Row
 */
@Composable
private fun DetailItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val haptics = rememberPimsHaptics()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress {
                haptics.selection()
                onClick()
            },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

